package com.stetits.core;

import com.stetits.core.docker.DockerClientFacade;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@TestConfiguration
@Profile("test")
public class TestDockerConfig {

    @Bean
    @Primary
    public DockerClientFacade testDockerClientFacade() {
        return new DockerClientFacade() {
            @Override
            public NetworkRef ensureNetwork(String name, Map<String, String> labels) {
                return new NetworkRef("test-network-id", name);
            }

            @Override
            public Optional<ContainerInfo> findContainerByName(String name) {
                return Optional.empty();
            }

            @Override
            public String createContainer(ContainerSpec spec) {
                return "test-container-" + spec.name();
            }

            @Override
            public void startContainer(String containerId) {
                // no-op
            }

            @Override
            public void stopContainer(String containerId) {
                // no-op
            }

            @Override
            public void removeContainer(String containerId, boolean force) {
                // no-op
            }

            @Override
            public void restartContainer(String containerId) {
                // no-op
            }

            @Override
            public List<ContainerInfo> listByStack(String stackId) {
                return List.of();
            }

            @Override
            public List<String> containerLogs(String containerId, int tail) {
                return List.of("Test log line 1", "Test log line 2");
            }
        };
    }
}
