package com.stetits.core.stack;

import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StackSpecResolver {

    private final StacksRepository stacks;
    private final StackVersionsRepository versions;
    private final StackSpecParser parser;

    public record Orders(String version, java.util.List<String> startOrder, java.util.List<String> stopOrder) {}

    public StackSpecResolver(StacksRepository stacks, StackVersionsRepository versions, StackSpecParser parser) {
        this.stacks = stacks;
        this.versions = versions;
        this.parser = parser;
    }

    public Optional<Orders> resolve(String stackId) throws Exception {
        var s = stacks.get(stackId);
        if (s.isEmpty() || s.get().currentVersion() == null || s.get().currentVersion().isBlank()) return Optional.empty();

        String version = s.get().currentVersion();
        String body = versions.getBodyJson(stackId, version).orElse(null);
        if (body == null) return Optional.empty();

        StackSpec spec = parser.parse(body);
        Map<String, List<String>> deps = new HashMap<>();
        for (var e : spec.services().entrySet()) deps.put(e.getKey(), Optional.ofNullable(e.getValue().dependsOn()).orElse(List.of()));

        List<String> start = ServiceGraph.topoSort(deps);
        List<String> stop = new ArrayList<>(start);
        Collections.reverse(stop);
        return Optional.of(new Orders(version, start, stop));
    }
}
