package com.stetits.core.docker;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface DockerClientFacade {

    record NetworkRef(String id, String name) {}
    record VolumeRef(String name) {}

    record ContainerInfo(String id, String name, String state, String status, Map<String,String> labels) {}

    record MountSpec(String volumeName, String containerPath, boolean readOnly) {}

    record ContainerSpec(
            String name,
            String hostname,
            String image,
            Map<String,String> env,
            Map<Integer,Integer> portsTcp,
            NetworkRef network,
            List<String> networkAliases,
            List<MountSpec> mounts,
            List<String> command,
            Map<String,String> labels
    ) {}

    NetworkRef ensureNetwork(String name, Map<String, String> labels);

    Optional<NetworkRef> findNetworkByName(String name);

    void removeNetwork(String networkId);

    VolumeRef ensureVolume(String volumeName, Map<String, String> labels);

    void removeVolume(String volumeName);

    Optional<ContainerInfo> findContainerByName(String name);

    List<ContainerInfo> listByStack(String stackId);

    InspectContainer inspectContainer(String containerId);

    void pullImageBlocking(String image, Duration timeout);

    String createContainer(ContainerSpec spec);

    void startContainer(String containerId);

    void stopContainer(String containerId);

    void removeContainer(String containerId, boolean force);

    List<String> containerLogs(String containerId, int tail);

    ExecResult execInContainer(String containerId, List<String> cmd, Duration timeout);

    record ExecResult(int exitCode, String stdout, String stderr) {}

    // Minimal inspect projection for diff + delete volume collection
    record InspectContainer(
            String id,
            String image,
            Map<String,String> env,
            Map<Integer,Integer> portsTcp,
            String networkName,
            List<String> aliases,
            String hostname,
            List<MountSpec> mounts,
            List<String> command
    ) {}
}
