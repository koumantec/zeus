package com.stetits.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.api.query.CommandLogsQueryController;
import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "orchestrator.worker.autostart=false")
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CommandLogsApiTest extends TestBase {
    @Autowired CommandsRepository commands;
    @Autowired CommandLogsRepository logsRepo;
    @Autowired MockMvc mvc;
    private ObjectMapper om = new ObjectMapper();

    @Test
    void logs_endpoint_respects_limit_and_returns_latest_first() throws Exception {
        long id = commands.enqueue("s1", "T1", "{}");

        // On simule 10 logs (append-only)
        for (int i = 1; i <= 10; i++) {
            logsRepo.append(id, "INFO", "log-" + i);
        }

        var res = mvc.perform(get("/commands/{id}/logs", id).param("limit", "5"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = om.readTree(res.getResponse().getContentAsString());
        JsonNode logs = root.get("logs");
        assertThat(logs).isNotNull();
        assertThat(logs.size()).isEqualTo(5);

        // le repository liste DESC (les plus récents d'abord) => log-10 doit apparaître avant log-9 etc.
        assertThat(logs.get(0).get("message").asText()).isEqualTo("log-10");
        assertThat(logs.get(1).get("message").asText()).isEqualTo("log-9");
    }
}
