package com.stetits.core.stack;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StackSpecParser {

    private final ObjectMapper om = new ObjectMapper();

    public StackSpec parse(String bodyJson) throws Exception {
        JsonNode root = om.readTree(bodyJson);
        JsonNode compose = root.path("compose");
        JsonNode services = compose.path("services");
        if (!services.isObject() || services.properties().isEmpty()) {
            throw new IllegalArgumentException("compose.services is empty");
        }

        Map<String, StackSpec.ServiceSpec> svcMap = new LinkedHashMap<>();
        services.properties().forEach(e -> {
            String name = e.getKey();
            JsonNode s = e.getValue();

            String image = s.path("image").asText(null);
            if (image == null || image.isBlank()) throw new IllegalArgumentException("service " + name + " missing image");

            Map<String,String> envMap = new LinkedHashMap<>();
            List<String> envList = new ArrayList<>();
            JsonNode env = s.get("environment");
            if (env != null) {
                if (env.isObject()) env.fields().forEachRemaining(x -> envMap.put(x.getKey(), x.getValue().asText("")));
                else if (env.isArray()) for (JsonNode x : env) envList.add(x.asText(""));
            }

            List<String> ports = new ArrayList<>();
            JsonNode p = s.get("ports");
            if (p != null && p.isArray()) for (JsonNode x : p) ports.add(x.asText(""));

            List<String> deps = new ArrayList<>();
            JsonNode d = s.get("depends_on");
            if (d != null && d.isArray()) for (JsonNode x : d) deps.add(x.asText(""));

            List<String> vols = new ArrayList<>();
            JsonNode v = s.get("volumes");
            if (v != null && v.isArray()) for (JsonNode x : v) vols.add(x.asText(""));

            svcMap.put(name, new StackSpec.ServiceSpec(image, envMap, envList, ports, deps, vols));
        });

        Map<String,Object> volumes = new LinkedHashMap<>();
        JsonNode vols = compose.get("volumes");
        if (vols != null && vols.isObject()) {
            vols.fields().forEachRemaining(e -> volumes.put(e.getKey(), new Object()));
        }

        return new StackSpec(svcMap, volumes);
    }
}
