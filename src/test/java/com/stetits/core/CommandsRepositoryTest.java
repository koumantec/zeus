package com.stetits.core;

import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.persistence.StacksRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CommandsRepositoryTest {

    @Autowired CommandsRepository commands;
    @Autowired StacksRepository stacks;

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
        // Assure stack existante (FK)
        if (stacks.get("s1").isEmpty()) stacks.insert("s1", "Stack 1");
    }

    @AfterEach
    void truncate(@Autowired JdbcTemplate jdbc) {
        jdbc.update("DELETE FROM command_logs");
        jdbc.update("DELETE FROM commands");
        jdbc.update("DELETE FROM stack_versions");
        jdbc.update("DELETE FROM stacks");
    }

    @Test
    void enqueue_createsPending() {
        long id = commands.enqueue("s1", "APPLY_STACK_VERSION", "{\"version\":\"v1\"}");
        var cmd = commands.get(id);
        assertThat(cmd).isPresent();
        assertThat(cmd.get().status()).isEqualTo("PENDING");
    }

    @Test
    void claimNextPending_claimsInOrder_andOnlyOneAtATime() {
        long id1 = commands.enqueue("s1", "T1", "{}");
        long id2 = commands.enqueue("s1", "T2", "{}");
        long id3 = commands.enqueue("s1", "T3", "{}");

        Optional<Long> c1 = commands.claimNextPending();
        assertThat(c1).contains(id1);

        // Tant que id1 est RUNNING, on ne doit pas pouvoir claim id2
        Optional<Long> c2 = commands.claimNextPending();
        assertThat(c2).isEmpty();

        commands.markDone(id1);

        Optional<Long> c3 = commands.claimNextPending();
        assertThat(c3).contains(id2);

        commands.markDone(id2);

        Optional<Long> c4 = commands.claimNextPending();
        assertThat(c4).contains(id3);
    }

    @Test
    void cancel_onlyIfPending() {
        long id = commands.enqueue("s1", "T1", "{}");
        assertThat(commands.cancelIfPending(id)).isTrue();

        // Déjà CANCELLED -> false
        assertThat(commands.cancelIfPending(id)).isFalse();
    }
}
