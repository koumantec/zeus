package com.stetits.core.docker.controller;

import com.stetits.core.docker.model.StackConfiguration;
import com.stetits.core.docker.service.StackConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class StackConfigController {

    @Autowired
    private StackConfigService stackConfigService;

    @GetMapping("/stack-config")
    public String showStackConfig(Model model) {
        // Load existing configuration if available
        StackConfiguration existingConfig = stackConfigService.loadStackConfiguration();
        if (existingConfig != null) {
            model.addAttribute("existingConfig", existingConfig);
            model.addAttribute("existingCommunity", existingConfig.getCommunity());
            model.addAttribute("existingSelections", stackConfigService.getSelectionsFromConfig(existingConfig));
        }
        return "stack-config";
    }

    @PostMapping("/stack-config/confirm")
    public String confirmStackConfig(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) List<String> selections,
            Model model) {
        
        if (community == null || selections == null || selections.isEmpty()) {
            model.addAttribute("error", "Veuillez sélectionner au moins une application");
            return "stack-config";
        }

        StackConfiguration config = parseSelections(community, selections);
        model.addAttribute("config", config);
        
        return "stack-confirmation";
    }

    @PostMapping("/stack-config/validate")
    public String validateStackConfig(
            @RequestParam(required = false) String community,
            @RequestParam(required = false) List<String> selections,
            Model model) {
        
        if (community == null || selections == null || selections.isEmpty()) {
            model.addAttribute("error", "Veuillez sélectionner au moins une application");
            return "stack-config";
        }

        StackConfiguration config = parseSelections(community, selections);
        
        // Save the configuration
        try {
            stackConfigService.saveStackConfiguration(config);
            model.addAttribute("success", "Configuration enregistrée avec succès");
            model.addAttribute("config", config);
            return "stack-confirmation";
        } catch (Exception e) {
            model.addAttribute("error", "Erreur lors de l'enregistrement de la configuration");
            return "stack-config";
        }
    }

    private StackConfiguration parseSelections(String community, List<String> selections) {
        StackConfiguration config = new StackConfiguration();
        config.setCommunity(community);

        // Group selections by platform and component
        for (String selection : selections) {
            String[] parts = selection.split("\\|");
            if (parts.length == 4) {
                String platformName = parts[0];
                String componentName = parts[1];
                String appName = parts[2];
                String appDetails = parts[3]; // version:archiveFile

                // Find or create platform
                StackConfiguration.Platform platform = config.getPlatforms().stream()
                        .filter(p -> p.getName().equals(platformName))
                        .findFirst()
                        .orElseGet(() -> {
                            StackConfiguration.Platform newPlatform = new StackConfiguration.Platform(platformName);
                            config.getPlatforms().add(newPlatform);
                            return newPlatform;
                        });

                // Find or create component
                StackConfiguration.Component component = platform.getComponents().stream()
                        .filter(c -> c.getName().equals(componentName))
                        .findFirst()
                        .orElseGet(() -> {
                            StackConfiguration.Component newComponent = new StackConfiguration.Component(componentName);
                            platform.getComponents().add(newComponent);
                            return newComponent;
                        });

                // Parse and add application
                String[] detailsParts = appDetails.split(":");
                String version = detailsParts.length > 0 ? detailsParts[0] : "";
                String archiveFile = detailsParts.length > 1 ? detailsParts[1] : "";
                
                StackConfiguration.Application app = new StackConfiguration.Application(appName, version, archiveFile);
                component.getApplications().add(app);
            }
        }

        return config;
    }
}
