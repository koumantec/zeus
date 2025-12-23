package com.stetits.core.docker.model;

import java.util.List;

public class ContainerInfo {
    private String id;
    private String name;
    private String image;
    private String status;
    private String state;
    private List<String> ports;
    private List<String> volumes;

    public ContainerInfo(String id, String name, String image, String status, String state, List<String> ports,
            List<String> volumes) {
        this.id = id;
        this.name = name;
        this.image = image;
        this.status = status;
        this.state = state;
        this.ports = ports;
        this.volumes = volumes;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getStatus() {
        return status;
    }

    public String getState() {
        return state;
    }

    public List<String> getPorts() {
        return ports;
    }

    public List<String> getVolumes() {
        return volumes;
    }
}
