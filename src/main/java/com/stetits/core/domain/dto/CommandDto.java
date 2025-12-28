package com.stetits.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record CommandDto(
        long id,
        @NotBlank String stackId,
        @NotBlank String type,
        @NotNull String payloadJson,
        @NotBlank String status,
        @NotNull Instant createdAt,
        Instant startedAt,
        Instant endedAt,
        String errorMessage
) {}

