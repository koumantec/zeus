package com.stetits.core.stack;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.persistence.StackVersionsRepository;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StackPlanner {

    private final StackVersionsRepository versions;
    private final StackSpecParser parser;
    private final DockerClientFacade docker;

    public StackPlanner(StackVersionsRepository versions, StackSpecParser parser, DockerClientFacade docker) {
        this.versions = versions;
        this.parser = parser;
        this.docker = docker;
    }

    public StackPlan plan(String stackId, String version) throws Exception {
        String body = versions.getBodyJson(stackId, version)
                .orElseThrow(() -> new IllegalArgumentException("StackVersion not found: " + stackId + "/" + version));

        StackSpec spec = parser.parse(body);
        String networkName = "core_" + stackId;

        List<StackPlan.Action> actions = new ArrayList<>();

        if (docker.findNetworkByName(networkName).isEmpty()) {
            actions.add(new StackPlan.Action("CREATE_NETWORK", networkName, ""));
        }

        // volumes: declared + implicit
        Set<String> vols = new LinkedHashSet<>(spec.volumes().keySet());
        for (var e : spec.services().entrySet()) {
            for (String m : Optional.ofNullable(e.getValue().volumes()).orElse(List.of())) {
                String vol = m.split(":")[0].trim();
                vols.add("core_" + stackId + "_" + vol);
            }
        }
        for (String v : vols) actions.add(new StackPlan.Action("ENSURE_VOLUME", v, ""));

        // ordering
        Map<String, List<String>> deps = new HashMap<>();
        for (var e : spec.services().entrySet()) deps.put(e.getKey(), Optional.ofNullable(e.getValue().dependsOn()).orElse(List.of()));
        List<String> order = ServiceGraph.topoSort(deps);

        // containers
        Set<String> desired = new HashSet<>();
        for (String svc : order) {
            desired.add("core_" + stackId + "_" + svc);
            String detail = "hostname=" + svc + ", alias=" + svc + ", command=" + spec.services().get(svc).command();
            actions.add(new StackPlan.Action("ENSURE_CONTAINER", svc, detail));
        }

        // orphans
        var actual = docker.listByStack(stackId);
        for (var c : actual) {
            if (!desired.contains(c.name())) {
                actions.add(new StackPlan.Action("REMOVE_ORPHAN_CONTAINER", c.name(), ""));
            }
        }

        return new StackPlan(stackId, version, actions);
    }
}
