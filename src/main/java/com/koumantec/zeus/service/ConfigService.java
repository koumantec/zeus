package com.koumantec.zeus.service;

import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ConfigService {

    private static final String CONFIG_DIR = "/root/.core";
    private static final String CONFIG_FILE = CONFIG_DIR + "/monitor.conf";

    public boolean isConfigured() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try {
                String content = Files.readString(file.toPath());
                return content != null && !content.trim().isEmpty();
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

    public void saveConfig(String gitLogin, String gitPassword, String harborLogin, String harborPassword)
            throws IOException {
        Files.createDirectories(Paths.get(CONFIG_DIR));
        String content = String.format("GIT_LOGIN=%s\nGIT_PASSWORD=%s\nHARBOR_LOGIN=%s\nHARBOR_PASSWORD=%s\n",
                gitLogin, gitPassword, harborLogin, harborPassword);
        Files.writeString(Paths.get(CONFIG_FILE), content);
    }

    public Map<String, String> readConfig() throws IOException {
        if (!isConfigured()) {
            return Collections.emptyMap();
        }
        Map<String, String> config = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(CONFIG_FILE));
        for (String line : lines) {
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                config.put(parts[0].trim(), parts[1].trim());
            }
        }
        return config;
    }
}
