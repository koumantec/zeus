package com.stetits.core.api.query;

import com.stetits.core.repository.CommandsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/commands")
public class CommandQueryController {

    private final CommandsRepository commands;

    public CommandQueryController(CommandsRepository commands) {
        this.commands = commands;
    }

    @GetMapping
    public Object list(@RequestParam(required = false) String stackId,
                       @RequestParam(defaultValue = "50") int limit) {
        return commands.list(Optional.ofNullable(stackId), limit);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable long id) {
        return commands.get(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Command not found")));
    }
}
