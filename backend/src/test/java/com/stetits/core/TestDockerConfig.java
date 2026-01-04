package com.stetits.core;

import com.stetits.core.docker.DockerClientFacade;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@TestConfiguration
@Profile("test")
public class TestDockerConfig {

    @Bean
    @Primary
    public DockerClientFacade testDockerClientFacade() {
        DockerClientFacade mock = mock(DockerClientFacade.class);
        
        when(mock.ensureNetwork(anyString(), anyMap()))
                .thenAnswer(invocation -> new DockerClientFacade.NetworkRef("test-network-id", invocation.getArgument(0)));
        
        when(mock.findNetworkByName(anyString()))
                .thenReturn(Optional.empty());
        
        doNothing().when(mock).removeNetwork(anyString());
        
        when(mock.ensureVolume(anyString(), anyMap()))
                .thenReturn(null);
        
        doNothing().when(mock).removeVolume(anyString());
        
        when(mock.findContainerByName(anyString()))
                .thenReturn(Optional.empty());
        
        when(mock.createContainer(any(DockerClientFacade.ContainerSpec.class)))
                .thenAnswer(invocation -> {
                    DockerClientFacade.ContainerSpec spec = invocation.getArgument(0);
                    return "test-container-" + spec.name();
                });
        
        doNothing().when(mock).startContainer(anyString());
        doNothing().when(mock).stopContainer(anyString());
        doNothing().when(mock).removeContainer(anyString(), anyBoolean());
        
        when(mock.listByStack(anyString()))
                .thenReturn(List.of());
        
        when(mock.inspectContainer(anyString()))
                .thenReturn(null);
        
        doNothing().when(mock).pullImageBlocking(anyString(), any(Duration.class));
        
        when(mock.containerLogs(anyString(), anyInt()))
                .thenReturn(List.of("Test log line 1", "Test log line 2"));
        
        when(mock.execInContainer(anyString(), anyList(), any(Duration.class)))
                .thenReturn(null);
        
        return mock;
    }
}
