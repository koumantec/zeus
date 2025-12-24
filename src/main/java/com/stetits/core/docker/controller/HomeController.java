package com.stetits.core.docker.controller;

import com.stetits.core.docker.model.StackConfiguration;
import com.stetits.core.docker.service.SettingsService;
import com.stetits.core.docker.service.StackConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private StackConfigService stackConfigService;

    @Autowired
    private com.stetits.core.docker.service.MockDockerService mockDockerService;

    // @Autowired
    // private com.stetits.core.docker.service.DockerService dockerService;

    @GetMapping("/")
    public String home(Model model) {
        if (!settingsService.isSettingsConfigured()) {
            return "redirect:/settings";
        }

        // Set current page for navigation
        model.addAttribute("currentPage", "dashboard");

        // Load Mock Docker containers for prototype
        try {
            model.addAttribute("containers", mockDockerService.listContainers());
            model.addAttribute("dockerAvailable", true);
        } catch (Exception e) {
            model.addAttribute("dockerAvailable", false);
            model.addAttribute("dockerError", e.getMessage());
        }

        // Load stack configuration if available
        StackConfiguration stackConfig = stackConfigService.loadStackConfiguration();
        if (stackConfig != null) {
            model.addAttribute("config", stackConfig);
            model.addAttribute("hasStackConfig", true);
        } else {
            model.addAttribute("hasStackConfig", false);
        }

        return "index";
    }
}
