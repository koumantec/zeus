package com.stetits.core.docker.controller;

import com.stetits.core.docker.service.SettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private SettingsService settingsService;

    @GetMapping("/")
    public String home() {
        if (!settingsService.isSettingsConfigured()) {
            return "redirect:/settings";
        }
        return "index";
    }
}
