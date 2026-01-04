package com.stetits.core.api.query;

import com.stetits.core.persistence.CommandLogsRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/commands")
public class CommandLogsQueryController {

    private final CommandLogsRepository commandLogsRepository;

    public CommandLogsQueryController(CommandLogsRepository commandLogsRepository) {
        this.commandLogsRepository = commandLogsRepository;
    }

    @GetMapping("/{id}/logs")
    public ResponseEntity<?> list(@PathVariable long id, @RequestParam(defaultValue = "200") int limit) {
        return ResponseEntity.ok(Map.of(
                "commandId", id,
                "logs", commandLogsRepository.list(id, limit)
        ));
    }
}
