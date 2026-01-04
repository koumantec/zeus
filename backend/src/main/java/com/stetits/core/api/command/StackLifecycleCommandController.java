package com.stetits.core.api.command;

import com.stetits.core.service.CommandService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/stacks")
public class StackLifecycleCommandController {

    private final CommandService commands;

    public StackLifecycleCommandController(CommandService commands) {
        this.commands = commands;
    }

    @PostMapping("/{stackId}/apply/{version}")
    public ResponseEntity<?> apply(@PathVariable String stackId, @PathVariable String version) {
        long id = commands.enqueueApply(stackId, version);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    @PostMapping("/{stackId}/start")
    public ResponseEntity<?> start(@PathVariable String stackId) {
        long id = commands.enqueueStart(stackId);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    @PostMapping("/{stackId}/stop")
    public ResponseEntity<?> stop(@PathVariable String stackId) {
        long id = commands.enqueueStop(stackId);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    @PostMapping("/{stackId}/restart")
    public ResponseEntity<?> restart(@PathVariable String stackId) {
        long id = commands.enqueueRestart(stackId);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    @PostMapping("/{stackId}/delete")
    public ResponseEntity<?> delete(@PathVariable String stackId) {
        long id = commands.enqueueDelete(stackId);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }
}
