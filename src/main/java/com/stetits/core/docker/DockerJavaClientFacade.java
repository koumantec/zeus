package com.stetits.core.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DockerJavaClientFacade implements DockerClientFacade {

    private final DockerClient docker;

    public DockerJavaClientFacade(DockerClient docker) {
        this.docker = docker;
    }
    @Override
    public Optional<ContainerInfo> findContainerByName(String name) {
        // Docker retourne les noms avec un "/" au début
        String dockerName = name.startsWith("/") ? name : "/" + name;

        var list = docker.listContainersCmd()
                .withShowAll(true)
                .exec();

        for (var c : list) {
            var names = c.getNames();
            if (names != null && Arrays.asList(names).contains(dockerName)) {
                return Optional.of(map(c));
            }
        }
        return Optional.empty();
    }

    @Override
    public NetworkRef ensureNetwork(String name, Map<String, String> labels) {
        var existing = docker.listNetworksCmd().withNameFilter(name).exec();
        if (existing != null && !existing.isEmpty()) {
            return new NetworkRef(existing.get(0).getId(), existing.get(0).getName());
        }
        String id = docker.createNetworkCmd().withName(name).withLabels(labels).exec().getId();
        return new NetworkRef(id, name);
    }

    @Override
    public String createContainer(ContainerSpec spec) {
        // Pull best-effort (on améliorera ensuite en attendant la fin)
        try { docker.pullImageCmd(spec.image()).start(); } catch (Exception ignored) {}

        List<String> env = spec.env() == null ? List.of() :
                spec.env().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();

        Ports portBindings = new Ports();
        List<ExposedPort> exposed = new ArrayList<>();

        if (spec.portsTcp() != null) {
            for (var e : spec.portsTcp().entrySet()) {
                int hostPort = e.getKey();
                int containerPort = e.getValue();
                ExposedPort ep = ExposedPort.tcp(containerPort);
                exposed.add(ep);
                portBindings.bind(ep, Ports.Binding.bindPort(hostPort));
            }
        }

        HostConfig hostConfig = HostConfig.newHostConfig().withPortBindings(portBindings);

        var resp = docker.createContainerCmd(spec.image())
                .withName(spec.name())
                .withEnv(env)
                .withLabels(spec.labels())
                .withExposedPorts(exposed)
                .withHostConfig(hostConfig)
                .exec();

        String id = resp.getId();

        if (spec.network() != null) {
            docker.connectToNetworkCmd()
                    .withContainerId(id)
                    .withNetworkId(spec.network().id())
                    .exec();
        }

        return id;
    }

    @Override public void startContainer(String containerId) { docker.startContainerCmd(containerId).exec(); }

    @Override public void stopContainer(String containerId) {
        try { docker.stopContainerCmd(containerId).withTimeout(5).exec(); } catch (Exception ignored) {}
    }

    @Override public void removeContainer(String containerId, boolean force) {
        docker.removeContainerCmd(containerId).withForce(force).exec();
    }

    @Override public void restartContainer(String containerId) {
        docker.restartContainerCmd(containerId).withtTimeout(5).exec();
    }

    @Override
    public List<String> containerLogs(String containerId, int tail) {
        var cb = new com.github.dockerjava.core.command.LogContainerResultCallback();
        try {
            docker.logContainerCmd(containerId)
                    .withStdOut(true).withStdErr(true)
                    .withTail(tail)
                    .exec(cb).awaitCompletion();
            return cb.toString().lines().toList(); // simple; on améliorera (timestamps)
        } catch (Exception e) {
            return List.of("Failed to fetch logs: " + e.getMessage());
        }
    }

    @Override
    public List<ContainerInfo> listByStack(String stackId) {
        var list = docker.listContainersCmd().withShowAll(true).exec();
        if (list == null) return List.of();

        return list.stream()
                .map(this::map)
                .filter(ci -> stackId.equals(ci.labels().get(DockerLabels.STACK_ID)))
                .sorted(Comparator.comparing(ContainerInfo::name))
                .toList();
    }

    private ContainerInfo map(com.github.dockerjava.api.model.Container c) {
        String name = (c.getNames() != null && c.getNames().length > 0) ? c.getNames()[0] : "";
        if (name.startsWith("/")) name = name.substring(1);

        Map<String,String> labels = c.getLabels() == null ? Map.of() : Map.copyOf(c.getLabels());
        return new ContainerInfo(c.getId(), name, c.getState(), c.getStatus(), labels);
    }
}
