package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.docker.DockerLabels;
import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.stack.*;
import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Component
public class ApplyStackVersionHandler implements CommandHandler {

    private final StackVersionsRepository versions;
    private final StacksRepository stacks;
    private final StackSpecParser parser;
    private final DockerClientFacade docker;

    @Value("${core.docker.pullTimeoutSeconds:180}")
    long pullTimeoutSeconds;

    public ApplyStackVersionHandler(StackVersionsRepository versions,
                                    StacksRepository stacks,
                                    StackSpecParser parser,
                                    DockerClientFacade docker) {
        this.versions = versions;
        this.stacks = stacks;
        this.parser = parser;
        this.docker = docker;
    }

    @Override public String type() { return "APPLY_STACK_VERSION"; }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        String stackId = ctx.stackId();
        String version = ctx.payload().path("version").asText(null);
        if (version == null || version.isBlank()) {
            ctx.error("Missing payload.version");
            throw new IllegalArgumentException("payload.version is required");
        }

        String bodyJson = versions.getBodyJson(stackId, version)
                .orElseThrow(() -> new IllegalArgumentException("StackVersion not found: " + stackId + "/" + version));

        StackSpec spec = parser.parse(bodyJson);

        String networkName = "core_" + stackId;
        var network = docker.ensureNetwork(networkName, DockerLabels.base(stackId, version, "_network"));
        ctx.info("Network ensured: " + network.name());

        // volumes: declared + implicit from mounts
        Set<String> volumeNames = new LinkedHashSet<>(spec.volumes().keySet());
        for (var e : spec.services().entrySet()) {
            for (String m : Optional.ofNullable(e.getValue().volumes()).orElse(List.of())) {
                String vol = parseVolumeName(m);
                if (vol != null) volumeNames.add(stackVolumeName(stackId, vol));
            }
        }

        for (String vol : volumeNames) {
            docker.ensureVolume(vol, DockerLabels.base(stackId, version, "_volume"));
            ctx.info("Volume ensured: " + vol);
        }

        // deps map
        Map<String, List<String>> deps = new HashMap<>();
        for (var e : spec.services().entrySet()) deps.put(e.getKey(), Optional.ofNullable(e.getValue().dependsOn()).orElse(List.of()));
        List<String> order = ServiceGraph.topoSort(deps);
        ctx.info("Service order: " + order);

        // actual containers by serviceName
        List<DockerClientFacade.ContainerInfo> actual = docker.listByStack(stackId);
        Map<String, DockerClientFacade.ContainerInfo> byName = new HashMap<>();
        for (var c : actual) byName.put(c.name(), c);

        Set<String> desiredContainerNames = new HashSet<>();

        // converge
        for (String serviceName : order) {
            StackSpec.ServiceSpec svc = spec.services().get(serviceName);

            String containerName = "core_" + stackId + "_" + serviceName;
            desiredContainerNames.add(containerName);

            // Pull blocking
            ctx.info("Pulling image: " + svc.image());
            docker.pullImageBlocking(svc.image(), Duration.ofSeconds(pullTimeoutSeconds));

            Map<String,String> env = mergeEnv(svc.environment(), svc.environmentList());
            Map<Integer,Integer> ports = parsePorts(svc.ports());

            List<DockerClientFacade.MountSpec> mounts = parseMounts(stackId, svc.volumes());

            Map<String,String> labels = DockerLabels.base(stackId, version, serviceName);
            labels.put("core.container_name", containerName);
            labels.put("core.network_name", network.name());

            var desired = new DockerClientFacade.ContainerSpec(
                    containerName,
                    serviceName,
                    svc.image(),
                    env,
                    ports,
                    network,
                    List.of(serviceName),
                    mounts,
                    svc.command(),
                    labels
            );

            var existing = docker.findContainerByName(containerName);

            if (existing.isPresent()) {
                var ins = docker.inspectContainer(existing.get().id());
                boolean same = DiffUtil.equalsSpec(ins, desired);

                if (!same) {
                    ctx.warn("Replacing container due to drift: " + containerName);
                    docker.stopContainer(existing.get().id());
                    docker.removeContainer(existing.get().id(), true);
                    String id = docker.createContainer(desired);
                    docker.startContainer(id);
                    ctx.info("Started (replaced) " + containerName + " id=" + id);
                } else {
                    ctx.info("No drift; ensuring running: " + containerName);
                    docker.startContainer(existing.get().id());
                }
            } else {
                ctx.info("Creating container: " + containerName);
                String id = docker.createContainer(desired);
                docker.startContainer(id);
                ctx.info("Started " + containerName + " id=" + id);
            }
        }

        // orphan cleanup: containers in actual but not in desired
        for (var c : actual) {
            if (!desiredContainerNames.contains(c.name())) {
                ctx.warn("Removing orphan container: " + c.name());
                docker.stopContainer(c.id());
                docker.removeContainer(c.id(), true);
            }
        }

        stacks.setCurrentVersion(stackId, version);
        ctx.info("Stack current_version set to " + version);
    }

    private static String stackVolumeName(String stackId, String vol) {
        // deterministic names to avoid collision: core_<stackId>_<vol>
        return "core_" + stackId + "_" + vol;
    }

    private static String parseVolumeName(String mount) {
        // format: "name:/path" or "name:/path:ro"
        if (mount == null) return null;
        String[] parts = mount.split(":");
        if (parts.length >= 2) return parts[0].trim();
        return null;
    }

    private static List<DockerClientFacade.MountSpec> parseMounts(String stackId, List<String> mounts) {
        if (mounts == null) return List.of();
        List<DockerClientFacade.MountSpec> out = new ArrayList<>();
        for (String m : mounts) {
            if (m == null || m.isBlank()) continue;
            String[] parts = m.split(":");
            if (parts.length < 2) continue;
            String vol = parts[0].trim();
            String path = parts[1].trim();
            boolean ro = parts.length >= 3 && "ro".equalsIgnoreCase(parts[2].trim());
            out.add(new DockerClientFacade.MountSpec(stackVolumeName(stackId, vol), path, ro));
        }
        return out;
    }

    private static Map<Integer,Integer> parsePorts(List<String> ports) {
        Map<Integer,Integer> out = new HashMap<>();
        if (ports == null) return out;
        for (String p : ports) {
            if (p == null) continue;
            String[] parts = p.split(":");
            if (parts.length == 2) {
                out.put(Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()));
            }
        }
        return out;
    }

    private static Map<String,String> mergeEnv(Map<String,String> envMap, List<String> envList) {
        Map<String,String> out = new HashMap<>();
        if (envMap != null) out.putAll(envMap);
        if (envList != null) {
            for (String s : envList) {
                int idx = s.indexOf('=');
                if (idx > 0) out.put(s.substring(0, idx), s.substring(idx + 1));
            }
        }
        return out;
    }
}
