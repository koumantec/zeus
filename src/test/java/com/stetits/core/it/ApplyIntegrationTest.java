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
    @Autowired ObjectMapper om;

    @BeforeEach
    void requireDocker() {
        DockerITSupport.requireDocker(docker);
    }

    @Test
    void apply_creates_containers_with_labels() throws Exception {
        String stackId = "it-" + UUID.randomUUID();
        stacks.insert(stackId, "IT stack");

        // Insérer une stack version (si tu n’as pas encore d’API d’écriture des versions, on peut le faire via repo si tu ajoutes une méthode insert)
        // => Recommandation: ajoute StackVersionsRepository.insert(...) en Phase 6.5.
        // En attendant: fais une insertion SQL directe dans un helper test, ou expose un endpoint admin.
        // Ici je pars du principe que tu as ajouté insert(...) (sinon je te donne le patch).
    }
}
