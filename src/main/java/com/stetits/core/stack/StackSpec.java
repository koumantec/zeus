package com.stetits.core.stack;

import java.util.List;
import java.util.Map;

public record StackSpec(
        Map<String, ServiceSpec> services,
        Map<String, Object> volumes // name -> {}
) {
    public record ServiceSpec(
            String image,
            Map<String,String> environment,
            List<String> environmentList,
            List<String> ports,        // ["8080:80"]
            List<String> dependsOn,    // ["db"]
            List<String> volumes       // ["db_data:/path", "db_data:/path:ro"]
    ) {}
}
