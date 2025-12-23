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

    @GetMapping("/")
    public String home(Model model) {
        if (!settingsService.isSettingsConfigured()) {
            return "redirect:/settings";
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
