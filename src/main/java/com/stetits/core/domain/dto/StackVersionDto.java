package com.stetits.core.domain.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record StackVersionDto(
        @NotBlank String stackId,
        @NotBlank String version,
        String parentVersion,
        @Valid @NotNull Metadata metadata,
        @Valid @NotNull Compose compose,
        Runtime runtime,     // optionnel (rempli par orchestrateur)
        Status status        // optionnel
) {
    public record Metadata(
            @NotNull Instant createdAt,
            @NotBlank String createdBy,
            String comment
    ) {}

    public record Compose(
            @NotBlank String version,
            @NotNull @Size(min=1) Map<String, Map<String, Object>> services,
            Map<String, Object> networks,
            Map<String, Object> volumes
    ) {}

    public record Runtime(
            Map<String, String> networks,
            Map<String, String> volumes,
            Map<String, Map<String, String>> containers
    ) {}

    public record Status(
            @NotBlank String desired,
            @NotBlank String actual,
            Instant lastUpdated
    ) {}
}

