package com.koumantec.coremonitor.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public class Dtos {

    @Data
    public static class ConfigRequest {
        private String gitLogin;
        private String gitPassword;
        private String harborLogin;
        private String harborPassword;
    }

    @Data
    @AllArgsConstructor
    public static class ConfigStatus {
        private boolean configured;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Container {
        private String name;
        private String command;
        private String state;
        private String status;
        private String ports;
    }
}
