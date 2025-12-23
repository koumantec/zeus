package com.stetits.core.docker.service;

import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Service
public class SettingsService {

    private static final String SETTINGS_DIR = System.getProperty("user.home") + "/.core";
    private static final String SETTINGS_FILE = SETTINGS_DIR + "/settings.conf";
    private static final String GITHUB_USERNAME_KEY = "github.stet.username";
    private static final String GITHUB_PASSWORD_KEY = "github.stet.password";

    public SettingsService() {
        ensureSettingsDirectoryExists();
    }

    private void ensureSettingsDirectoryExists() {
        try {
            Path dirPath = Paths.get(SETTINGS_DIR);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create settings directory", e);
        }
    }

    public boolean isSettingsConfigured() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists() || file.length() == 0) {
            return false;
        }

        Properties properties = loadSettings();
        String username = properties.getProperty(GITHUB_USERNAME_KEY);
        String password = properties.getProperty(GITHUB_PASSWORD_KEY);

        return username != null && !username.trim().isEmpty() 
            && password != null && !password.trim().isEmpty();
    }

    public Properties loadSettings() {
        Properties properties = new Properties();
        File file = new File(SETTINGS_FILE);
        
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load settings", e);
            }
        }
        
        return properties;
    }

    public void saveSettings(String githubUsername, String githubPassword) {
        Properties properties = new Properties();
        properties.setProperty(GITHUB_USERNAME_KEY, githubUsername);
        properties.setProperty(GITHUB_PASSWORD_KEY, githubPassword);

        try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
            properties.store(fos, "Core Monitor Settings");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save settings", e);
        }
    }

    public String getGithubUsername() {
        return loadSettings().getProperty(GITHUB_USERNAME_KEY, "");
    }

    public String getGithubPassword() {
        return loadSettings().getProperty(GITHUB_PASSWORD_KEY, "");
    }
}
