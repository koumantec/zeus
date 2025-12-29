package com.stetits.core.docker;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DockerClientFacade {

    record NetworkRef(String id, String name) {}

    NetworkRef ensureNetwork(String name, Map<String, String> labels);

    Optional<ContainerInfo> findContainerByName(String name);

    String createContainer(ContainerSpec spec);

    void startContainer(String containerId);

    void stopContainer(String containerId);

    void removeContainer(String containerId, boolean force);

    void restartContainer(String containerId);

    List<ContainerInfo> listByStack(String stackId);

    List<String> containerLogs(String containerId, int tail);

    record ContainerInfo(String id, String name, String state, String status, Map<String,String> labels) {}

    record ContainerSpec(
            String name,
            String image,
            Map<String,String> env,
            Map<Integer,Integer> portsTcp,    // hostPort -> containerPort
            NetworkRef network,              // id + name
            Map<String,String> labels
    ) {}
}
