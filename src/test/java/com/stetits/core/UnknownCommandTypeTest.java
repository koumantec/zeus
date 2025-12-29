package com.stetits.core;

import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.worker.CommandWorker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class UnknownCommandTypeTest extends TestBase {

    @Autowired
    CommandsRepository commands;
    @Autowired
    CommandWorker worker;
    @Autowired
    CommandLogsRepository logs;

    @Test
    void unknown_command_type_is_marked_failed_and_logged() throws Exception {
        long id = commands.enqueue("s1", "UNKNOWN_TYPE", "{}");

        boolean processed = worker.processOne();
        assertThat(processed).isTrue();

        var cmd = commands.get(id).orElseThrow();
        assertThat(cmd.status()).isEqualTo("FAILED");

        var l = logs.list(id, 200);
        assertThat(l).isNotEmpty();

        // On vérifie qu’on a bien un message clair (selon ce qui est écris dans CommandExecutionService)
        assertThat(l.stream().anyMatch(x ->
                x.message().contains("No handler") || x.message().contains("No handler registered")
        )).isTrue();
    }
}
