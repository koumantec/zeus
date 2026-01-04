package com.stetits.core;

import com.stetits.core.persistence.CommandsRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CommandsRepositoryTest extends TestBase {

    @Autowired CommandsRepository commands;

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
