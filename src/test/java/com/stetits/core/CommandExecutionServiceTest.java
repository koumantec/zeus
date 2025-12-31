package com.stetits.core;

import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.worker.CommandExecutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDockerConfig.class)
class CommandExecutionServiceTest extends TestBase {

    @Autowired CommandsRepository commandsRepository;
    @Autowired CommandLogsRepository logs;
    @Autowired CommandExecutionService exec;

    @Test
    void execute_apply_requiresVersion_payload() throws Exception {
        long id = commandsRepository.enqueue("s1", "APPLY_STACK_VERSION", "{}");

        // On simule le claim => RUNNING
        var claimed = commandsRepository.claimNextPending();
        assertThat(claimed).contains(id);

        assertThatThrownBy(() -> exec.execute(id))
                .isInstanceOf(IllegalArgumentException.class);

        // Ici, le worker mettra FAILED. On teste juste l'exception au niveau service.
    }

    @Test
    void logs_are_written_by_handler() throws Exception {
        long id = commandsRepository.enqueue("s1", "APPLY_STACK_VERSION", "{\"version\":\"v1\"}");
        var claimed = commandsRepository.claimNextPending();
        assertThat(claimed).contains(id);

        exec.execute(id);

        var l = logs.list(id, 50);
        assertThat(l).isNotEmpty();
        // Vérifier qu'il y a des logs (le handler ApplyStackVersionHandler écrit des logs)
        assertThat(l.stream().anyMatch(x -> x.message().contains("Network ensured") || x.message().contains("Service start order"))).isTrue();
    }
}
