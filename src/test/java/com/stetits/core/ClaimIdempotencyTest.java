package com.stetits.core;

import com.stetits.core.persistence.CommandsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "orchestrator.worker.autostart=false")
@ActiveProfiles("test")
class ClaimIdempotencyTest extends TestBase {

    @Autowired CommandsRepository commands;

    @Test
    void claim_is_idempotent_while_running() {
        long c1 = commands.enqueue("s1", "T1", "{}");

        Optional<Long> first = commands.claimNextPending();
        assertThat(first).contains(c1);

        // Tant que c1 est RUNNING, aucun second claim
        Optional<Long> second = commands.claimNextPending();
        assertThat(second).isEmpty();

        commands.markDone(c1);

        // Plus rien à claim
        Optional<Long> third = commands.claimNextPending();
        assertThat(third).isEmpty();
    }
}
