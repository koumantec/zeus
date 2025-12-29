package com.stetits.core.api.query;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.persistence.StacksRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/stacks")
public class StackRuntimeQueryController {

    private final StacksRepository stacks;
    private final DockerClientFacade docker;

    public StackRuntimeQueryController(StacksRepository stacks, DockerClientFacade docker) {
        this.stacks = stacks;
        this.docker = docker;
    }

    @GetMapping("/{stackId}/containers")
    public ResponseEntity<?> containers(@PathVariable String stackId) {
        if (stacks.get(stackId).isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Stack not found"));
        }
        return ResponseEntity.ok(Map.of(
                "stackId", stackId,
                "containers", docker.listByStack(stackId)
        ));
    }

    @GetMapping("/{stackId}/status")
    public ResponseEntity<?> status(@PathVariable String stackId) {
        if (stacks.get(stackId).isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Stack not found"));
        var containers = docker.listByStack(stackId);
        return ResponseEntity.ok(Map.of(
                "stackId", stackId,
                "containers", containers,
                "summary", Map.of(
                        "total", containers.size(),
                        "running", containers.stream().filter(c -> "running".equalsIgnoreCase(c.state())).count()
                )
        ));
    }

    @GetMapping("/{stackId}/logs")
    public ResponseEntity<?> logs(@PathVariable String stackId,
                                  @RequestParam String service,
                                  @RequestParam(defaultValue = "200") int tail) {
        if (stacks.get(stackId).isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Stack not found"));

        String name = "core_" + stackId + "_" + service;
        var c = docker.findContainerByName(name);
        if (c.isEmpty()) return ResponseEntity.status(404).body(Map.of("error", "Container not found for service=" + service));

        return ResponseEntity.ok(Map.of(
                "stackId", stackId,
                "service", service,
                "containerId", c.get().id(),
                "lines", docker.containerLogs(c.get().id(), tail)
        ));
    }
}
