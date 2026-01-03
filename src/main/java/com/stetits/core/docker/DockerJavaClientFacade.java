package com.stetits.core.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class DockerJavaClientFacade implements DockerClientFacade {

    private final DockerClient docker;

    @Value("${core.docker.pullTimeoutSeconds:180}")
    long pullTimeoutSeconds;

    public DockerJavaClientFacade(DockerClient docker) {
        this.docker = docker;
    }

    @Override
    public NetworkRef ensureNetwork(String name, Map<String, String> labels) {
        var existing = docker.listNetworksCmd().withNameFilter(name).exec();
        if (existing != null && !existing.isEmpty()) {
            return new NetworkRef(existing.get(0).getId(), existing.get(0).getName());
        }
        String id = docker.createNetworkCmd()
                .withName(name)
                .withLabels(labels)
                .exec().getId();
        return new NetworkRef(id, name);
    }

    @Override
    public Optional<NetworkRef> findNetworkByName(String name) {
        var existing = docker.listNetworksCmd().withNameFilter(name).exec();
        if (existing != null && !existing.isEmpty()) {
            return Optional.of(new NetworkRef(existing.get(0).getId(), existing.get(0).getName()));
        }
        return Optional.empty();
    }

    @Override
    public void removeNetwork(String networkId) {
        docker.removeNetworkCmd(networkId).exec();
    }

    @Override
    public VolumeRef ensureVolume(String volumeName, Map<String, String> labels) {
        try {
            docker.inspectVolumeCmd(volumeName).exec();
            return new VolumeRef(volumeName);
        } catch (Exception ignored) {
            docker.createVolumeCmd().withName(volumeName).withLabels(labels).exec();
            return new VolumeRef(volumeName);
        }
    }

    @Override
    public void removeVolume(String volumeName) {
        docker.removeVolumeCmd(volumeName).exec();
    }

    @Override
    public Optional<ContainerInfo> findContainerByName(String name) {
        String dockerName = name.startsWith("/") ? name : "/" + name;
        var list = docker.listContainersCmd().withShowAll(true).exec();
        for (var c : list) {
            var names = c.getNames();
            if (names != null && Arrays.asList(names).contains(dockerName)) {
                return Optional.of(map(c));
            }
        }
        return Optional.empty();
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

    @Override
    public InspectContainer inspectContainer(String containerId) {
        InspectContainerResponse r = docker.inspectContainerCmd(containerId).exec();

        // image
        String image = r.getConfig() != null ? r.getConfig().getImage() : null;

        // env
        Map<String,String> env = new HashMap<>();
        String[] envArr = r.getConfig() != null ? r.getConfig().getEnv() : null;
        if (envArr != null) {
            for (String e : envArr) {
                int idx = e.indexOf('=');
                if (idx > 0) env.put(e.substring(0, idx), e.substring(idx + 1));
            }
        }

        // hostname
        String hostname = r.getConfig() != null ? r.getConfig().getHostName() : null;

        // mounts (named volumes)
        List<MountSpec> mounts = new ArrayList<>();
        if (r.getMounts() != null) {
            for (InspectContainerResponse.Mount m : r.getMounts()) {
                // Vérifier si c'est un volume nommé (pas un bind mount)
                // Un volume nommé a un Name non null
                String volumeName = m.getName();
                Volume destination = m.getDestination();
                Boolean readWrite = m.getRW();
                
                // Si c'est un volume nommé (pas un bind mount qui aurait Source au lieu de Name)
                if (volumeName != null && destination != null) {
                    String destPath = destination.getPath();
                    if (destPath != null) {
                        mounts.add(new MountSpec(volumeName, destPath, !Boolean.TRUE.equals(readWrite)));
                    }
                }
            }
        }

        // ports host -> container
        Map<Integer,Integer> ports = new HashMap<>();
        var netSettings = r.getNetworkSettings();
        if (netSettings != null && netSettings.getPorts() != null) {
            Ports bindings = netSettings.getPorts();
            Map<ExposedPort, Ports.Binding[]> pb = bindings.getBindings();
            if (pb != null) {
                for (var e : pb.entrySet()) {
                    ExposedPort ep = e.getKey();
                    Ports.Binding[] bs = e.getValue();
                    if (bs != null) {
                        for (Ports.Binding b : bs) {
                            if (b != null && b.getHostPortSpec() != null) {
                                try {
                                    int hostPort = Integer.parseInt(b.getHostPortSpec());
                                    ports.put(hostPort, ep.getPort());
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                }
            }
        }

        // network name + aliases
        String networkName = null;
        List<String> aliases = new ArrayList<>();
        if (netSettings != null && netSettings.getNetworks() != null && !netSettings.getNetworks().isEmpty()) {
            // take first network (stack network)
            var first = netSettings.getNetworks().entrySet().iterator().next();
            networkName = first.getKey();
            var al = first.getValue().getAliases();
            if (al != null) aliases.addAll(al);
        }

        List<String> cmd = new ArrayList<>();
        String[] cmdArr = (r.getConfig() != null) ? r.getConfig().getCmd() : null;
        if (cmdArr != null) cmd.addAll(Arrays.asList(cmdArr));

        return new InspectContainer(containerId, image, env, ports, networkName, aliases, hostname, mounts, cmd);
    }

    @Override
    public void pullImageBlocking(String image, Duration timeout) {
        try {
            docker.pullImageCmd(image)
                    .exec(new PullImageResultCallback())
                    .awaitCompletion(timeout.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Pull failed for " + image + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String createContainer(ContainerSpec spec) {
        // Exposed ports + port bindings
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

        // Binds for volumes
        List<Bind> binds = new ArrayList<>();
        if (spec.mounts() != null) {
            for (var m : spec.mounts()) {
                Volume v = new Volume(m.containerPath());
                AccessMode mode = m.readOnly() ? AccessMode.ro : AccessMode.rw;
                binds.add(new Bind(m.volumeName(), v, mode));
            }
        }

        HostConfig hostConfig = HostConfig.newHostConfig()
                .withPortBindings(portBindings)
                .withBinds(binds);

        // env list
        List<String> env = spec.env() == null ? List.of() :
                spec.env().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList();

        var cmd = spec.command();

        var create = docker.createContainerCmd(spec.image())
                .withName(spec.name())
                .withHostName(spec.hostname())
                .withEnv(env)
                .withLabels(spec.labels())
                .withExposedPorts(exposed)
                .withHostConfig(hostConfig);

        if (cmd != null && !cmd.isEmpty()) {
            create = create.withCmd(cmd.toArray(String[]::new));
        }

        var resp = create.exec();

        String id = resp.getId();

        // connect network with alias
        if (spec.network() != null) {
            ContainerNetwork containerNetwork = new ContainerNetwork().withAliases(spec.networkAliases() == null ? List.of() : spec.networkAliases());
            docker.connectToNetworkCmd()
                    .withContainerId(id)
                    .withNetworkId(spec.network().id())
                    .withContainerNetwork(containerNetwork)
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

    @Override
    public List<String> containerLogs(String containerId, int tail) {
        try (var cb = new com.github.dockerjava.core.command.LogContainerResultCallback()) {
            docker.logContainerCmd(containerId)
                    .withStdOut(true).withStdErr(true)
                    .withTail(tail)
                    .exec(cb).awaitCompletion();
            String s = cb.toString();
            return s.isBlank() ? List.of() : s.lines().toList();
        } catch (Exception e) {
            return List.of("Failed to fetch logs: " + e.getMessage());
        }
    }

    @Override
    public ExecResult execInContainer(String containerId, List<String> cmd, Duration timeout) {
        try {
            var exec = docker.execCreateCmd(containerId)
                    .withAttachStdout(true)
                    .withAttachStderr(true)
                    .withCmd(cmd.toArray(String[]::new))
                    .exec();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();

            docker.execStartCmd(exec.getId())
                    .exec(new ExecStartResultCallback(out, err))
                    .awaitCompletion(timeout.toSeconds(), TimeUnit.SECONDS);

            Integer code = docker.inspectExecCmd(exec.getId()).exec().getExitCode();
            int exit = code == null ? -1 : code;

            return new ExecResult(exit,
                    out.toString(StandardCharsets.UTF_8),
                    err.toString(StandardCharsets.UTF_8));
        } catch (Exception e) {
            return new ExecResult(-1, "", "exec failed: " + e.getMessage());
        }
    }

    private ContainerInfo map(com.github.dockerjava.api.model.Container c) {
        String name = (c.getNames() != null && c.getNames().length > 0) ? c.getNames()[0] : "";
        if (name.startsWith("/")) name = name.substring(1);
        Map<String,String> labels = c.getLabels() == null ? Map.of() : Map.copyOf(c.getLabels());
        return new ContainerInfo(c.getId(), name, c.getState(), c.getStatus(), labels);
    }
}
