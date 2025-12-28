package com.stetits.core.api.command;

import com.stetits.core.service.CommandService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @PostMapping("/stacks/{stackId}/apply/{version}")
    public ResponseEntity<?> apply(@PathVariable String stackId, @PathVariable String version) {
        long id = commandService.enqueueApply(stackId, version);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    @PostMapping("/stacks/{stackId}/rollback/{version}")
    public ResponseEntity<?> rollback(@PathVariable String stackId, @PathVariable String version) {
        long id = commandService.enqueueRollback(stackId, version);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    @PostMapping("/stacks/{stackId}/delete")
    public ResponseEntity<?> delete(@PathVariable String stackId) {
        long id = commandService.enqueueDelete(stackId);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    /**
     * DEPLOY_APP payload exemple:
     * {
     *   "targetService": "wildfly",
     *   "artifactPath": "/uploads/app.war",
     *   "strategy": "copy"
     * }
     */
    @PostMapping("/stacks/{stackId}/deploy")
    public ResponseEntity<?> deploy(@PathVariable String stackId, @RequestBody Map<String, Object> payload) {
        long id = commandService.enqueueDeploy(stackId, payload);
        return ResponseEntity.accepted().body(Map.of("commandId", id, "status", "PENDING"));
    }

    @PostMapping("/commands/{commandId}/cancel")
    public ResponseEntity<?> cancel(@PathVariable long commandId) {
        boolean ok = commandService.cancel(commandId);
        return ok
                ? ResponseEntity.ok(Map.of("commandId", commandId, "status", "CANCELLED"))
                : ResponseEntity.status(409).body(Map.of("error", "Command cannot be cancelled (not PENDING)"));
    }
}
