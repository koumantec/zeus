package com.stetits.core.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.DockerClient;
import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.worker.CommandWorker;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("it")
@SpringBootTest(properties = "orchestrator.worker.autostart=false")
@AutoConfigureMockMvc
@ActiveProfiles("docker")
class EndToEndApplyIT {

    @Autowired DockerClient docker;
    @Autowired MockMvc mvc;
    ObjectMapper om = new ObjectMapper();
    @Autowired CommandsRepository commands;
    @Autowired CommandWorker worker;


    static String dbPath;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws Exception {
        dbPath = "target/test-" + java.util.UUID.randomUUID() + ".db";
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + dbPath);
    }

    @AfterAll
    static void cleanup() throws Exception {
        java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(dbPath));
    }


    @BeforeEach
    void requireDocker() {
        DockerITSupport.requireDocker(docker);
    }

    @AfterEach
    void truncate(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM command_logs");
        jdbc.update("DELETE FROM commands");
        jdbc.update("DELETE FROM stack_versions");
        jdbc.update("DELETE FROM stacks");
    }

    @Test
    void create_stack_version_apply_status_and_cleanup() throws Exception {
        String stackId = "it-" + UUID.randomUUID();

        // 1) Create stack
        mvc.perform(post("/stacks")
                        .contentType("application/json")
                        .content("{\"stackId\":\""+stackId+"\",\"name\":\"IT Stack\"}"))
                .andExpect(status().isCreated());

        // 2) Create version (2 services with depends_on)
        String body = """
      {
        "compose": {
          "services": {
            "db": { "image": "redis:7-alpine" },
            "web": { "image": "nginx:alpine", "ports": ["18080:80"], "depends_on": ["db"] }
          }
        }
      }
      """;

        var verRes = mvc.perform(post("/stacks/{id}/versions", stackId)
                        .contentType("application/json")
                        .content("{\"body\":"+ body +",\"createdBy\":\"it\",\"comment\":\"e2e\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String version = om.readTree(verRes.getResponse().getContentAsString()).get("version").asText();
        assertThat(version).isNotBlank();

        // 3) Enqueue apply
        var cmdRes = mvc.perform(post("/stacks/{id}/apply/{version}", stackId, version))
                .andExpect(status().isAccepted())
                .andReturn();

        long cmdId = om.readTree(cmdRes.getResponse().getContentAsString()).get("commandId").asLong();

        // 4) Process command synchronously (no background)
        assertThat(worker.processOne()).isTrue();

        var cmd = commands.get(cmdId).orElseThrow();
        assertThat(cmd.status()).isEqualTo("DONE");

        // 5) Status endpoint should show containers
        mvc.perform(get("/stacks/{id}/status", stackId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.total").value(2));

        // 6) Cleanup via delete command
        var delRes = mvc.perform(post("/stacks/{id}/delete", stackId))
                .andExpect(status().isAccepted())
                .andReturn();
        long delId = om.readTree(delRes.getResponse().getContentAsString()).get("commandId").asLong();

        assertThat(worker.processOne()).isTrue();
        assertThat(commands.get(delId).orElseThrow().status()).isEqualTo("DONE");
    }
}
