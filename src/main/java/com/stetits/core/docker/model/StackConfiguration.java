package com.stetits.core.docker.model;

import java.util.ArrayList;
import java.util.List;

public class StackConfiguration {
    private String community; // fr or be
    private List<Platform> platforms;

    public StackConfiguration() {
        this.platforms = new ArrayList<>();
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public List<Platform> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<Platform> platforms) {
        this.platforms = platforms;
    }

    public static class Platform {
        private String name;
        private List<Component> components;

        public Platform() {
            this.components = new ArrayList<>();
        }

        public Platform(String name) {
            this.name = name;
            this.components = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Component> getComponents() {
            return components;
        }

        public void setComponents(List<Component> components) {
            this.components = components;
        }
    }

    public static class Component {
        private String name;
        private List<Application> applications;

        public Component() {
            this.applications = new ArrayList<>();
        }

        public Component(String name) {
            this.name = name;
            this.applications = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Application> getApplications() {
            return applications;
        }

        public void setApplications(List<Application> applications) {
            this.applications = applications;
        }
    }

    public static class Application {
        private String name;
        private String version;
        private String archiveFile;

        public Application() {
        }

        public Application(String name, String version, String archiveFile) {
            this.name = name;
            this.version = version;
            this.archiveFile = archiveFile;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getArchiveFile() {
            return archiveFile;
        }

        public void setArchiveFile(String archiveFile) {
            this.archiveFile = archiveFile;
        }
    }
}
