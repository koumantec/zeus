package com.stetits.core;

import com.stetits.core.persistence.CommandsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class CommandsOrderingTest extends TestBase {

    @Autowired
    private CommandsRepository commands;

    @Test
    void cancelled_does_not_block_next_command() {
        long c1 = commands.enqueue("s1", "APPLY_STACK_VERSION", "{\"version\":\"v1\"}");
        long c2 = commands.enqueue("s1", "ROLLBACK_STACK", "{\"targetVersion\":\"v0\"}");

        boolean cancelled = commands.cancelIfPending(c1);
        assertThat(cancelled).isTrue();

        Optional<Long> claimed = commands.claimNextPending();
        assertThat(claimed).contains(c2);
    }
}
