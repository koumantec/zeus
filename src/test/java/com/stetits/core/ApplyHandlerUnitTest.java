package com.stetits.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.stack.StackSpecParser;
import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.handlers.ApplyStackVersionHandler;
import com.stetits.core.persistence.CommandLogsRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplyHandlerUnitTest {

    @Test
    void apply_creates_network_and_container_and_updates_current_version() throws Exception {
        // Mocks
        StackVersionsRepository versions = mock(StackVersionsRepository.class);
        StacksRepository stacks = mock(StacksRepository.class);
        DockerClientFacade docker = mock(DockerClientFacade.class);
        CommandLogsRepository logs = mock(CommandLogsRepository.class);
        
        String versionBody = """
      {"stackId":"s1","version":"v1","compose":{"services":{"web":{"image":"nginx:alpine","ports":["8080:80"]}}}}
    """;
        
        // Configuration des mocks
        when(versions.getBodyJson("s1", "v1")).thenReturn(Optional.of(versionBody));
        when(stacks.get("s1")).thenReturn(Optional.of(new com.stetits.core.domain.dto.StackDto("s1", "Stack 1", null)));
        
        // Mock Docker - capture des appels
        Map<String, DockerClientFacade.NetworkRef> createdNetworks = new HashMap<>();
        Map<String, DockerClientFacade.ContainerInfo> createdContainers = new HashMap<>();
        
        when(docker.ensureNetwork(anyString(), anyMap())).thenAnswer(invocation -> {
            String name = invocation.getArgument(0);
            DockerClientFacade.NetworkRef ref = new DockerClientFacade.NetworkRef("net-" + name, name);
            createdNetworks.put(name, ref);
            return ref;
        });
        
        when(docker.findContainerByName(anyString())).thenReturn(Optional.empty());
        
        when(docker.createContainer(any(DockerClientFacade.ContainerSpec.class))).thenAnswer(invocation -> {
            DockerClientFacade.ContainerSpec spec = invocation.getArgument(0);
            String id = "c-" + spec.name();
            DockerClientFacade.ContainerInfo info = new DockerClientFacade.ContainerInfo(id, spec.name(), "created", "Created", spec.labels());
            createdContainers.put(spec.name(), info);
            return id;
        });
        
        doNothing().when(docker).startContainer(anyString());
        doNothing().when(docker).pullImageBlocking(anyString(), any());
        
        when(logs.list(anyLong(), anyInt())).thenReturn(List.of());
        doNothing().when(logs).append(anyLong(), anyString(), anyString());
        
        ArgumentCaptor<String> versionCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(stacks).setCurrentVersion(eq("s1"), versionCaptor.capture());
        
        var parser = new StackSpecParser();
        var om = new ObjectMapper();
        
        var handler = new ApplyStackVersionHandler(versions, stacks, parser, docker);
        var ctx = new CommandContext(1L, "s1", "APPLY_STACK_VERSION", om.readTree("{\"version\":\"v1\"}"), logs);

        handler.execute(ctx);

        // Vérifications
        assertThat(createdNetworks).containsKey("core_s1");
        assertThat(createdContainers).containsKey("core_s1_web");
        assertThat(versionCaptor.getValue()).isEqualTo("v1");
        
        verify(docker).ensureNetwork(eq("core_s1"), anyMap());
        verify(docker).createContainer(any(DockerClientFacade.ContainerSpec.class));
        verify(docker).startContainer(anyString());
        verify(stacks).setCurrentVersion("s1", "v1");
    }
}
