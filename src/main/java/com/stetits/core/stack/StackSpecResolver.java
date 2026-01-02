package com.stetits.core.stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StackSpecResolver {

    private final StacksRepository stacks;
    private final StackVersionsRepository versions;
    private final ObjectMapper om;

    public StackSpecResolver(StacksRepository stacks, StackVersionsRepository versions, ObjectMapper om) {
        this.stacks = stacks;
        this.versions = versions;
        this.om = om;
    }

    public record StackOrder(String version, List<String> startOrder, List<String> stopOrder) {}

    public Optional<StackOrder> resolveOrders(String stackId) throws Exception {
        var s = stacks.get(stackId);
        if (s.isEmpty() || s.get().currentVersion() == null || s.get().currentVersion().isBlank()) {
            return Optional.empty();
        }
        String version = s.get().currentVersion();
        String body = versions.getBodyJson(stackId, version).orElse(null);
        if (body == null) return Optional.empty();

        JsonNode root = om.readTree(body);
        JsonNode services = root.path("compose").path("services");
        if (!services.isObject()) return Optional.empty();

        Map<String, List<String>> deps = new HashMap<>();
        services.properties().forEach(e -> {
            String svc = e.getKey();
            JsonNode def = e.getValue();
            List<String> d = new ArrayList<>();
            if (def.has("depends_on") && def.get("depends_on").isArray()) {
                for (JsonNode x : def.get("depends_on")) d.add(x.asText());
            }
            deps.put(svc, d);
        });

        List<String> start = ServiceGraph.topoSort(deps);
        List<String> stop = new ArrayList<>(start);
        Collections.reverse(stop);

        return Optional.of(new StackOrder(version, start, stop));
    }
}
