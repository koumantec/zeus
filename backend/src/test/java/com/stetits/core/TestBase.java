package com.stetits.core;

import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.service.StackVersionService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDockerConfig.class)
public class TestBase {

    @Autowired
    StacksRepository stacks;

    @Autowired
    StackVersionsRepository stackVersions;

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
    void setup() {
        if (stacks.get("s1").isEmpty()) {
            stacks.insert("s1", "Stack 1");
            // Créer des versions par défaut pour les tests
            String defaultBody = "{\"compose\":{\"services\":{\"web\":{\"image\":\"nginx:alpine\"}}}}";
            try {
                String sha = StackVersionService.sha256Hex(defaultBody);

                // Créer v0 (version initiale)
                stackVersions.insert(new StackVersionsRepository.StackVersionRow(
                    "s1",
                    "v0",
                    null,
                    defaultBody,
                    sha,
                    "test",
                    "Initial test version"
                ));
                // Créer v1 (version suivante)
                stackVersions.insert(new StackVersionsRepository.StackVersionRow(
                    "s1",
                    "v1",
                    "v0",
                    defaultBody,
                    sha,
                    "test",
                    "Default test version"
                ));
            } catch (Exception e) {
                throw new RuntimeException("Failed to create default stack version", e);
            }
        }
    }

    @AfterEach
    void truncate(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM command_logs");
        jdbc.update("DELETE FROM commands");
        jdbc.update("DELETE FROM stack_versions");
        jdbc.update("DELETE FROM stacks");
    }
}
