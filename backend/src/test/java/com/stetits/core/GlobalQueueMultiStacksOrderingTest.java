package com.stetits.core;

import com.stetits.core.persistence.CommandsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class GlobalQueueMultiStacksOrderingTest extends TestBase {

    @Autowired
    CommandsRepository commands;

    @BeforeEach
    void setup() {
        if (stacks.get("s1").isEmpty()) stacks.insert("s1", "Stack 1");
        if (stacks.get("s2").isEmpty()) stacks.insert("s2", "Stack 2");
    }

    @Test
    void global_queue_enforces_strict_order_across_stacks() {
        long c1 = commands.enqueue("s1", "T1", "{}"); // plus ancien
        long c2 = commands.enqueue("s2", "T2", "{}"); // plus récent

        Optional<Long> first = commands.claimNextPending();
        assertThat(first).contains(c1);

        // Tant que c1 est RUNNING, on ne doit pas pouvoir claim c2 même si c2 est sur une autre stack
        // ceci pour éviter notamment des conflits de ports lorsqu'on éteint par exemple une V1 puis on démarre la V2
        Optional<Long> second = commands.claimNextPending();
        assertThat(second).isEmpty();

        commands.markDone(c1);

        Optional<Long> third = commands.claimNextPending();
        assertThat(third).contains(c2);
    }
}
