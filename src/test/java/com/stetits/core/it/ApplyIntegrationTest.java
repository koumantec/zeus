package com.stetits.core.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.worker.CommandWorker;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

@Tag("it")
@SpringBootTest(properties = "orchestrator.worker.autostart=false")
@ActiveProfiles("docker") // ou "test" si datasource OK
class ApplyIntegrationTest {

    @Autowired com.github.dockerjava.api.DockerClient docker;
    @Autowired DockerClientFacade facade;

    @Autowired StacksRepository stacks;
    @Autowired StackVersionsRepository versions;
    @Autowired CommandsRepository commands;
    @Autowired CommandWorker worker;
    private ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void requireDocker() {
        DockerITSupport.requireDocker(docker);
    }

    @Test
    void apply_creates_containers_with_labels() throws Exception {
        String stackId = "it-" + UUID.randomUUID();
        stacks.insert(stackId, "IT stack");
        // TODO: à finaliser
    }
}
