package com.stetits.core.worker.handlers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.docker.DockerLabels;
import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.stack.ServiceGraph;
import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ApplyStackVersionHandler implements CommandHandler {

    private final StackVersionsRepository versions;
    private final StacksRepository stacks;
    private final DockerClientFacade docker;
    private final ObjectMapper om = new ObjectMapper();

    public ApplyStackVersionHandler(StackVersionsRepository versions,
                                    StacksRepository stacks,
                                    DockerClientFacade docker) {
        this.versions = versions;
        this.stacks = stacks;
        this.docker = docker;
    }

    @Override
    public String type() {
        return "APPLY_STACK_VERSION";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        String stackId = ctx.stackId();
        String version = ctx.payload().path("version").asText(null);
        if (version == null || version.isBlank()) throw new IllegalArgumentException("payload.version is required");

        String bodyJson = versions.getBodyJson(stackId, version)
                .orElseThrow(() -> new IllegalArgumentException("StackVersion not found: " + stackId + "/" + version));

        JsonNode root = om.readTree(bodyJson);
        JsonNode services = root.path("compose").path("services");
        if (!services.isObject() || !services.fields().hasNext()) throw new IllegalArgumentException("compose.services is empty");

        String networkName = "core_" + stackId;
        var network = docker.ensureNetwork(networkName, DockerLabels.base(stackId, version, "_network"));
        ctx.info("Network ensured: " + network.name());

        // dependencies map
        Map<String, java.util.List<String>> deps = new java.util.HashMap<>();
        services.fields().forEachRemaining(e -> {
            String svc = e.getKey();
            JsonNode def = e.getValue();
            java.util.List<String> d = new java.util.ArrayList<>();
            if (def.has("depends_on") && def.get("depends_on").isArray()) {
                for (JsonNode x : def.get("depends_on")) d.add(x.asText());
            }
            deps.put(svc, d);
        });

        var order = ServiceGraph.topoSort(deps);
        ctx.info("Service start order: " + order);

        for (String serviceName : order) {
            JsonNode svc = services.get(serviceName);
            String image = svc.path("image").asText(null);
            if (image == null || image.isBlank()) throw new IllegalArgumentException("service " + serviceName + " missing image");

            var env = parseEnv(svc.path("environment"));
            var ports = parsePorts(svc.path("ports"));

            String containerName = "core_" + stackId + "_" + serviceName;

            var labels = new java.util.HashMap<>(DockerLabels.base(stackId, version, serviceName));
            labels.put("core.container_name", containerName);
            labels.put("core.network_name", network.name());

            var spec = new DockerClientFacade.ContainerSpec(containerName, image, env, ports, network, labels);

            var existing = docker.findContainerByName(containerName);
            if (existing.isPresent()) {
                boolean replace = !version.equals(existing.get().labels().get(DockerLabels.STACK_VERSION));
                if (replace) {
                    ctx.warn("Replacing " + containerName + " due to version change");
                    docker.stopContainer(existing.get().id());
                    docker.removeContainer(existing.get().id(), true);
                    String id = docker.createContainer(spec);
                    docker.startContainer(id);
                    ctx.info("Started " + containerName + " id=" + id);
                } else {
                    ctx.info("Ensuring running " + containerName);
                    docker.startContainer(existing.get().id());
                }
            } else {
                ctx.info("Creating " + containerName);
                String id = docker.createContainer(spec);
                docker.startContainer(id);
                ctx.info("Started " + containerName + " id=" + id);
            }
        }

        stacks.setCurrentVersion(stackId, version);
        ctx.info("Stack current_version updated to " + version);
    }


    private boolean mustReplace(DockerClientFacade.ContainerInfo actual, DockerClientFacade.ContainerSpec desired) {
        // MVP “diff” : si version/service/image/env/ports changent -> replace
        var labels = actual.labels();
        if (!desired.labels().get(DockerLabels.STACK_VERSION).equals(labels.get(DockerLabels.STACK_VERSION))) return true;
        // image/env/ports ne sont pas visibles dans ContainerInfo sans inspect,
        // donc MVP: on remplace sur changement de version uniquement.
        // Amélioration phase suivante: inspect container + compare.
        return false;
    }

    private Map<String,String> parseEnv(JsonNode node) {
        Map<String,String> env = new HashMap<>();
        if (node == null || node.isMissingNode() || node.isNull()) return env;

        // support: map {KEY:VAL} ou list ["KEY=VAL"]
        if (node.isObject()) {
            node.fields().forEachRemaining(e -> env.put(e.getKey(), e.getValue().asText("")));
        } else if (node.isArray()) {
            for (JsonNode x : node) {
                String s = x.asText("");
                int idx = s.indexOf('=');
                if (idx > 0) env.put(s.substring(0, idx), s.substring(idx + 1));
            }
        }
        return env;
    }

    private Map<Integer,Integer> parsePorts(JsonNode node) {
        Map<Integer,Integer> ports = new HashMap<>();
        if (node == null || node.isMissingNode() || node.isNull()) return ports;

        // support: list ["8080:8080"]
        if (node.isArray()) {
            for (JsonNode x : node) {
                String s = x.asText("");
                String[] parts = s.split(":");
                if (parts.length == 2) {
                    int host = Integer.parseInt(parts[0].trim());
                    int cont = Integer.parseInt(parts[1].trim());
                    ports.put(host, cont);
                }
            }
        }
        return ports;
    }
}
