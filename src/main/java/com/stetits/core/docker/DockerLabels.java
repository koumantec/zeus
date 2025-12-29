package com.stetits.core.docker;

import java.util.Map;

public final class DockerLabels {
    private DockerLabels() {}

    public static final String ORCH = "core.orchestrator";
    public static final String STACK_ID = "core.stack_id";
    public static final String STACK_VERSION = "core.stack_version";
    public static final String SERVICE = "core.service";
    public static final String MANAGED_BY = "core.managed_by";

    public static Map<String, String> base(String stackId, String version, String service) {
        return Map.of(
                ORCH, "true",
                MANAGED_BY, "core-control",
                STACK_ID, stackId,
                STACK_VERSION, version,
                SERVICE, service
        );
    }
}
