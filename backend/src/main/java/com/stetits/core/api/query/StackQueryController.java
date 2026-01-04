package com.stetits.core.api.query;

import com.stetits.core.persistence.StackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/stacks")
public class StackQueryController {

    private final StacksRepository stacks;
    private final StackVersionsRepository versions;

    public StackQueryController(StacksRepository stacks, StackVersionsRepository versions) {
        this.stacks = stacks;
        this.versions = versions;
    }

    @GetMapping
    public Object list() {
        return stacks.list();
    }

    @GetMapping("/{stackId}")
    public ResponseEntity<?> get(@PathVariable String stackId) {
        return stacks.get(stackId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Stack not found")));
    }

    @GetMapping("/{stackId}/versions")
    public ResponseEntity<?> listVersions(@PathVariable String stackId) {
        var st = stacks.get(stackId);
        if (st.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Stack not found"));
        }
        return ResponseEntity.ok(Map.of(
                "stackId", stackId,
                "currentVersion", st.get().currentVersion(),
                "versions", versions.listVersions(stackId)
        ));
    }

    @GetMapping("/{stackId}/versions/{version}")
    public ResponseEntity<?> getVersion(@PathVariable String stackId, @PathVariable String version) {
        var body = versions.getBodyJson(stackId, version);
        if (body.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "StackVersion not found"));
        }
        // Pour l'instant on renvoie le JSON stocké (phase suivante: DTO complet)
        return ResponseEntity.ok(Map.of("stackId", stackId, "version", version, "bodyJson", body.get()));
    }
}
