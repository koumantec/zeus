package com.koumantec.coremonitor.service;

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

    public List<String> getContainers() {
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

}
