package com.koumantec.coremonitor.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

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

    /**
     * Représente tous les choix de l'utilisateur dans l'onglet "Composition de la stack".
     * Les champs correspondent aux "name" des inputs HTML afin de permettre le binding JSON->Objet.
     * Certains champs peuvent être null selon le parcours (seuls les champs visibles sont envoyés côté frontend).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StackCompositionRequest {
        // fr | be
        private String community;
        // [core], [acore], [core, acore]
        private List<String> platforms;
        // [ihm], [flux], [ihm, flux]
        private List<String> components;
        // [inf], [plbinf], [inf, plbinf]
        private List<String> webapps;
    }
}
