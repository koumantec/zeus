package com.stetits.core.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record StackDto(@NotBlank String stackId, @NotBlank String name, String currentVersion) {
}

