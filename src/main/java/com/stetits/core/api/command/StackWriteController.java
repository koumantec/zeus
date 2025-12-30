package com.stetits.core.api.command;

import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.service.StackVersionService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/stacks")
public class StackWriteController {

    private final StacksRepository stacks;
    private final StackVersionService versionService;

    public StackWriteController(StacksRepository stacks, StackVersionService versionService) {
        this.stacks = stacks;
        this.versionService = versionService;
    }

    public record CreateStackRequest(@NotBlank String stackId, @NotBlank String name) {}

    @PostMapping
    public ResponseEntity<?> createStack(@RequestBody CreateStackRequest req) {
        if (stacks.get(req.stackId()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Stack already exists"));
        }
        stacks.insert(req.stackId(), req.name());
        return ResponseEntity.status(201).body(Map.of("stackId", req.stackId(), "name", req.name()));
    }

    @PostMapping("/{stackId}/versions")
    public ResponseEntity<?> createVersion(@PathVariable String stackId,
                                           @RequestBody StackVersionService.CreateVersionRequest req) throws Exception {
        var res = versionService.create(stackId, req);
        return ResponseEntity.status(201).body(res);
    }
}
