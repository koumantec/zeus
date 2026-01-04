package com.stetits.core.docker;

import java.util.HashMap;
import java.util.Map;

public final class DockerLabels {
    private DockerLabels() {}

    public static final String ORCH = "core.orchestrator";
    public static final String STACK_ID = "core.stack_id";
    public static final String STACK_VERSION = "core.stack_version";
    public static final String SERVICE = "core.service";
    public static final String MANAGED_BY = "core.managed_by";

    public static Map<String, String> base(String stackId, String version, String service) {
        Map<String,String> m = new HashMap<>();
        m.put(ORCH, "true");
        m.put(MANAGED_BY, "core-control");
        m.put(STACK_ID, stackId);
        if (version != null) m.put(STACK_VERSION, version);
        if (service != null) m.put(SERVICE, service);
        return m;
    }
}
