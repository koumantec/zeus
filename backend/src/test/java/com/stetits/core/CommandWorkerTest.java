package com.stetits.core;

import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.worker.CommandWorker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CommandWorkerTest extends TestBase {

    @Autowired CommandsRepository commandsRepository;
    @Autowired CommandLogsRepository commandLogsRepository;
    @Autowired CommandWorker worker;

    @Test
    void worker_processes_commands_in_order() throws Exception {
        long c1 = commandsRepository.enqueue("s1", "APPLY_STACK_VERSION", "{\"version\":\"v1\"}");
        long c2 = commandsRepository.enqueue("s1", "ROLLBACK_STACK", "{\"targetVersion\":\"v0\"}");

        assertThat(worker.processOne()).isTrue(); // traite c1
        assertThat(commandsRepository.get(c1).get().status()).isEqualTo("DONE");

        assertThat(worker.processOne()).isTrue(); // traite c2
        assertThat(commandsRepository.get(c2).get().status()).isEqualTo("DONE");

        assertThat(commandLogsRepository.list(c1, 50)).isNotEmpty();
        assertThat(commandLogsRepository.list(c2, 50)).isNotEmpty();
    }

    @Test
    void worker_marks_failed_on_exception() throws Exception {
        long c1 = commandsRepository.enqueue("s1", "APPLY_STACK_VERSION", "{}"); // manque version

        assertThat(worker.processOne()).isTrue();
        assertThat(commandsRepository.get(c1).get().status()).isEqualTo("FAILED");
    }
}
