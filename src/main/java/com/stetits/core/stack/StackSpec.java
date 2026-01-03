package com.stetits.core.stack;

import java.util.List;
import java.util.Map;

public record StackSpec(
        Map<String, ServiceSpec> services,
        Map<String, Object> volumes
) {
    public record ServiceSpec(
            String image,
            Map<String,String> environment,
            List<String> environmentList,
            List<String> ports,
            List<String> dependsOn,
            List<String> volumes,
            List<String> command   // ["sh","-lc","sleep 300"] ; null si absent
    ) {}
}
