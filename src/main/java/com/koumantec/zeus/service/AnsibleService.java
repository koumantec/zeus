package com.koumantec.zeus.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnsibleService {

    @Value("${ansible.host:local}")
    private String ansibleHost;

    @Value("${demo.mode:true}")
    private String demoMode;

    public List<String> getContainers() {
        if ("true".equalsIgnoreCase(demoMode)) {
            // Mock data matches server.js structure but as raw strings for parsed
            // processing or direct objects
            // The controller will map this to objects, here we return raw or objects?
            // Let's return raw fake output to simulate shell output or just objects.
            // server.js returned objects directly in demo mode.
            // But for consistency let's return objects from here? Or a structured list.
            return List.of(); // Demo mode handled in Controller usually? Or here.
        }

        String command = String.format("ansible %s -m shell -a \"docker-compose ps\"", ansibleHost);
        return executeCommand(command);
    }

    public List<String> executeCommand(String command) {
        List<String> output = new ArrayList<>();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("bash", "-c", command);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                output.add(line);
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
    }

    public boolean isDemoMode() {
        return "true".equalsIgnoreCase(demoMode);
    }
}
