package com.koumantec.coremonitor.controller;

import com.koumantec.coremonitor.dto.Dtos;
import com.koumantec.coremonitor.service.AnsibleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/containers")
public class ContainerController {

    private final AnsibleService ansibleService;

    public ContainerController(AnsibleService ansibleService) {
        this.ansibleService = ansibleService;
    }

    @GetMapping
    public List<Dtos.Container> getContainers() {
        List<String> output = ansibleService.getContainers();
        return parseContainers(output);
    }

    private List<Dtos.Container> parseContainers(List<String> lines) {
        List<Dtos.Container> containers = new ArrayList<>();
        boolean parsing = false;

        for (String line : lines) {
            if (line.contains("Name") && line.contains("Command") && line.contains("State")) {
                parsing = true;
                continue;
            }
            if (line.startsWith("---"))
                continue;
            if (!parsing)
                continue;
            if (line.trim().isEmpty())
                continue;

            String[] parts = line.split("\\s{2,}");
            if (parts.length >= 3) {
                containers.add(new Dtos.Container(
                        parts[0],
                        parts[1],
                        parts[2],
                        "Unknown", // Status often combined or missing in simple split if strictly 3 parts
                        parts.length > 3 ? parts[3] : ""));
            }
        }
        return containers;
    }
}
