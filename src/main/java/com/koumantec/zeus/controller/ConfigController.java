package com.koumantec.zeus.controller;

import com.koumantec.zeus.dto.Dtos;
import com.koumantec.zeus.service.ConfigService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @GetMapping("/status")
    public ResponseEntity<Dtos.ConfigStatus> getConfigStatus() {
        return ResponseEntity.ok(new Dtos.ConfigStatus(configService.isConfigured()));
    }

    @PostMapping
    public ResponseEntity<Map<String, Boolean>> saveConfig(@RequestBody Dtos.ConfigRequest request) {
        try {
            configService.saveConfig(request.getGitLogin(), request.getGitPassword(), 
                                   request.getHarborLogin(), request.getHarborPassword());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> getConfig() {
        try {
            Map<String, String> config = configService.readConfig();
            if (config.isEmpty()) {
                return ResponseEntity.ok(Map.of());
            }
            return ResponseEntity.ok(Map.of(
                "gitLogin", config.getOrDefault("GIT_LOGIN", ""),
                "gitPassword", config.getOrDefault("GIT_PASSWORD", ""),
                "harborLogin", config.getOrDefault("HARBOR_LOGIN", ""),
                "harborPassword", config.getOrDefault("HARBOR_PASSWORD", "")
            ));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
