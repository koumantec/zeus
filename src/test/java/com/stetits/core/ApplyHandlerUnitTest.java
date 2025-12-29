package com.stetits.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.handlers.ApplyStackVersionHandler;
import com.stetits.core.persistence.CommandLogsRepository;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplyHandlerUnitTest {

    @Test
    void apply_creates_network_and_container_and_updates_current_version() throws Exception {
        // fakes
        var versions = new FakeVersionsRepo("""
      {"stackId":"s1","version":"v1","compose":{"services":{"web":{"image":"nginx:alpine","ports":["8080:80"]}}}}
    """);
        var stacks = new FakeStacksRepo();
        stacks.insert("s1", "Stack 1");

        var docker = new FakeDocker();
        var om = new ObjectMapper();

        var handler = new ApplyStackVersionHandler(
                versions,
                stacks,
                docker);

        var logs = mock(CommandLogsRepository.class);
        when(logs.list(anyLong(), anyInt())).thenReturn(List.of());
        doNothing().when(logs).append(anyLong(), anyString(), anyString());


        var ctx = new CommandContext(1L, "s1", "APPLY_STACK_VERSION", om.readTree("{\"version\":\"v1\"}"), (CommandLogsRepository) logs);

        handler.execute(ctx);

        assertThat(docker.networks).containsKey("core_s1");
        assertThat(docker.containersByName).containsKey("core_s1_web");
        assertThat(stacks.currentVersion.get()).isEqualTo("v1");
    }

    // ---- fakes minimalistes ----
    static class FakeDocker implements DockerClientFacade {
        Map<String,String> networks = new HashMap<>();
        Map<String,ContainerInfo> containersByName = new HashMap<>();
        long seq = 1;

        @Override public String ensureNetwork(String name, Map<String, String> labels) {
            return networks.computeIfAbsent(name, n -> "net-" + n);
        }
        @Override public Optional<ContainerInfo> findContainerByName(String name) {
            return Optional.ofNullable(containersByName.get(name));
        }
        @Override public String createContainer(ContainerSpec spec) {
            String id = "c-" + (seq++);
            containersByName.put(spec.name(), new ContainerInfo(id, spec.name(), "created", "Created", spec.labels()));
            return id;
        }
        @Override public void startContainer(String containerId) { /* no-op */ }
        @Override public void stopAndRemoveContainer(String containerId) {
            containersByName.values().removeIf(c -> c.id().equals(containerId));
        }
        @Override public List<ContainerInfo> listByStack(String stackId) {
            return containersByName.values().stream()
                    .filter(c -> stackId.equals(c.labels().get("core.stack_id")))
                    .toList();
        }
    }

    static class FakeVersionsRepo extends StackVersionsRepository {
        final String body;
        FakeVersionsRepo(String body) { super(null);this.body = body; }
        @Override public Optional<String> getBodyJson(String stackId, String version) { return Optional.of(body); }
        @Override public List<String> listVersions(String stackId) { return List.of("v1"); }
    }

    static class FakeStacksRepo extends StacksRepository {
        final AtomicReference<String> currentVersion = new AtomicReference<>(null);
        final Set<String> ids = new HashSet<>();

        public FakeStacksRepo() {
            super(null);
        }

        @Override public List<com.stetits.core.domain.dto.StackDto> list() { return List.of(); }
        @Override public Optional<com.stetits.core.domain.dto.StackDto> get(String stackId) {
            return ids.contains(stackId) ? Optional.of(new com.stetits.core.domain.dto.StackDto(stackId, "x", currentVersion.get())) : Optional.empty();
        }
        @Override public void insert(String stackId, String name) { ids.add(stackId); }
        @Override public void setCurrentVersion(String stackId, String version) { currentVersion.set(version); }
    }
}
