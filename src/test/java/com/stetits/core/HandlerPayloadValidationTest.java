package com.stetits.core;

import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.worker.CommandWorker;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class HandlerPayloadValidationTest {
    record Case(String type, String payloadJson, String expectedLogContains) {}

    @Autowired CommandsRepository commands;
    @Autowired CommandLogsRepository logs;
    @Autowired CommandWorker worker;

    static Stream<Case> badPayloadCases() {
        return Stream.of(
                new Case("APPLY_STACK_VERSION", "{}", "payload.version"),
                new Case("ROLLBACK_STACK", "{}", "payload.targetVersion")
        );
    }

    @ParameterizedTest
    @MethodSource("badPayloadCases")
    void invalid_payload_must_fail_and_be_logged(Case c) throws Exception {
        long id = commands.enqueue("s1", c.type(), c.payloadJson());

        boolean processed = worker.processOne();
        assertThat(processed).isTrue();

        var cmd = commands.get(id).orElseThrow();
        assertThat(cmd.status()).isEqualTo("FAILED");

        var l = logs.list(id, 200);
        assertThat(l).isNotEmpty();

        // On veut au moins un message utile qui pointe le champ manquant
        assertThat(l.stream().anyMatch(x -> x.message().contains(c.expectedLogContains()))).isTrue();
    }
}
