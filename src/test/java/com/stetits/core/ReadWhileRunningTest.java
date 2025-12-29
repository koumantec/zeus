package com.stetits.core;

import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.worker.CommandExecutionService;
import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "orchestrator.worker.autostart=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReadWhileRunningTest extends TestBase {
    @Autowired CommandsRepository commands;
    @Autowired CommandExecutionService exec;

    private ObjectMapper om = new ObjectMapper();

    @Autowired MockMvc mvc;

    // latches fournis par la config de test
    @Autowired CountDownLatch startedLatch;
    @Autowired CountDownLatch releaseLatch;

    @TestConfiguration
    static class BlockingHandlerConfig {

        @Bean
        public CountDownLatch startedLatch() {
            return new CountDownLatch(1);
        }

        @Bean
        public CountDownLatch releaseLatch() {
            return new CountDownLatch(1);
        }

        @Bean
        public CommandHandler blockingHandler(CountDownLatch startedLatch, CountDownLatch releaseLatch) {
            return new CommandHandler() {
                @Override public String type() { return "BLOCKING_READ_TEST"; }

                @Override
                public void execute(CommandContext ctx) throws Exception {
                    ctx.info("Blocking handler started");
                    startedLatch.countDown();
                    // attend que le test autorise la fin
                    releaseLatch.await(5, TimeUnit.SECONDS);
                    ctx.info("Blocking handler released");
                }
            };
        }
    }

    @Test
    void read_endpoints_work_while_command_is_running() throws Exception {
        long id = commands.enqueue("s1", "BLOCKING_READ_TEST", "{}");
        var claimed = commands.claimNextPending();
        assertThat(claimed).contains(id);

        Thread t = new Thread(() -> {
            try {
                exec.execute(id);       // exécute handler qui bloque
                commands.markDone(id);
            } catch (Exception e) {
                commands.markFailed(id, e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            }
        });
        t.start();

        // On attend que le handler ait démarré et soit "bloqué"
        assertThat(startedLatch.await(2, TimeUnit.SECONDS)).isTrue();

        // READ doit répondre OK pendant l'exécution
        mvc.perform(get("/commands").param("limit", "50"))
                .andExpect(status().isOk());

        // READ détail doit montrer RUNNING
        var res = mvc.perform(get("/commands/{id}", id))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = om.readTree(res.getResponse().getContentAsString());
        assertThat(root.get("status").asText()).isEqualTo("RUNNING");

        // On libère l'exécution
        releaseLatch.countDown();
        t.join(2000);

        var cmd = commands.get(id).orElseThrow();
        assertThat(cmd.status()).isEqualTo("DONE");
    }
}
