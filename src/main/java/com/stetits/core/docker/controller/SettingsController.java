package com.stetits.core.docker.controller;

import com.stetits.core.docker.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SettingsController {

    @Autowired
    private SettingsService settingsService;

    @GetMapping("/settings")
    public String settings(Model model) {
        model.addAttribute("githubUsername", settingsService.getGithubUsername());
        model.addAttribute("githubPassword", settingsService.getGithubPassword());
        return "settings";
    }

    @PostMapping("/settings")
    public String saveSettings(
            @RequestParam("githubUsername") String githubUsername,
            @RequestParam("githubPassword") String githubPassword,
            RedirectAttributes redirectAttributes) {
        
        try {
            settingsService.saveSettings(githubUsername, githubPassword);
            redirectAttributes.addFlashAttribute("success", "Paramètres enregistrés avec succès");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur lors de l'enregistrement des paramètres");
            return "redirect:/settings";
        }
    }
}
