package com.stetits.core.docker.service;

import com.stetits.core.docker.model.StackConfiguration;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
public class StackConfigService {

    private static final String SETTINGS_DIR = System.getProperty("user.home") + "/.core";
    private static final String STACK_CONFIG_FILE = SETTINGS_DIR + "/stack.conf";

    public StackConfigService() {
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

    public boolean hasStackConfiguration() {
        File file = new File(STACK_CONFIG_FILE);
        return file.exists() && file.length() > 0;
    }

    public void saveStackConfiguration(StackConfiguration config) {
        Properties properties = new Properties();
        
        // Save community
        properties.setProperty("stack.community", config.getCommunity());
        
        // Save platforms, components and applications
        int platformIndex = 0;
        for (StackConfiguration.Platform platform : config.getPlatforms()) {
            String platformKey = "stack.platform." + platformIndex;
            properties.setProperty(platformKey + ".name", platform.getName());
            
            int componentIndex = 0;
            for (StackConfiguration.Component component : platform.getComponents()) {
                String componentKey = platformKey + ".component." + componentIndex;
                properties.setProperty(componentKey + ".name", component.getName());
                
                int appIndex = 0;
                for (StackConfiguration.Application app : component.getApplications()) {
                    String appKey = componentKey + ".app." + appIndex;
                    properties.setProperty(appKey + ".name", app.getName());
                    properties.setProperty(appKey + ".version", app.getVersion());
                    properties.setProperty(appKey + ".archiveFile", app.getArchiveFile());
                    appIndex++;
                }
                properties.setProperty(componentKey + ".appCount", String.valueOf(appIndex));
                componentIndex++;
            }
            properties.setProperty(platformKey + ".componentCount", String.valueOf(componentIndex));
            platformIndex++;
        }
        properties.setProperty("stack.platformCount", String.valueOf(platformIndex));

        try (FileOutputStream fos = new FileOutputStream(STACK_CONFIG_FILE)) {
            properties.store(fos, "Stack Configuration");
        } catch (IOException e) {
            throw new RuntimeException("Failed to save stack configuration", e);
        }
    }

    public StackConfiguration loadStackConfiguration() {
        if (!hasStackConfiguration()) {
            return null;
        }

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(STACK_CONFIG_FILE)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load stack configuration", e);
        }

        StackConfiguration config = new StackConfiguration();
        config.setCommunity(properties.getProperty("stack.community", ""));

        int platformCount = Integer.parseInt(properties.getProperty("stack.platformCount", "0"));
        for (int p = 0; p < platformCount; p++) {
            String platformKey = "stack.platform." + p;
            String platformName = properties.getProperty(platformKey + ".name");
            StackConfiguration.Platform platform = new StackConfiguration.Platform(platformName);

            int componentCount = Integer.parseInt(properties.getProperty(platformKey + ".componentCount", "0"));
            for (int c = 0; c < componentCount; c++) {
                String componentKey = platformKey + ".component." + c;
                String componentName = properties.getProperty(componentKey + ".name");
                StackConfiguration.Component component = new StackConfiguration.Component(componentName);

                int appCount = Integer.parseInt(properties.getProperty(componentKey + ".appCount", "0"));
                for (int a = 0; a < appCount; a++) {
                    String appKey = componentKey + ".app." + a;
                    String appName = properties.getProperty(appKey + ".name");
                    String version = properties.getProperty(appKey + ".version");
                    String archiveFile = properties.getProperty(appKey + ".archiveFile");
                    
                    StackConfiguration.Application app = new StackConfiguration.Application(appName, version, archiveFile);
                    component.getApplications().add(app);
                }
                platform.getComponents().add(component);
            }
            config.getPlatforms().add(platform);
        }

        return config;
    }

    public List<String> getSelectionsFromConfig(StackConfiguration config) {
        List<String> selections = new ArrayList<>();
        
        if (config == null) {
            return selections;
        }

        for (StackConfiguration.Platform platform : config.getPlatforms()) {
            for (StackConfiguration.Component component : platform.getComponents()) {
                for (StackConfiguration.Application app : component.getApplications()) {
                    // Format: platform|component|appName|version:archiveFile
                    String selection = platform.getName() + "|" + 
                                     component.getName() + "|" + 
                                     app.getName() + "|" + 
                                     app.getVersion() + ":" + app.getArchiveFile();
                    selections.add(selection);
                }
            }
        }
        
        return selections;
    }
}
