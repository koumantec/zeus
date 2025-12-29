package com.stetits.core.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

/**
 * On garde CommandDto pour les APIs READ, mais l’exécuteur travaille avec CommandRow minimal.
 * @param id
 * @param stackId
 * @param type
 * @param payloadJson
 * @param status
 * @param createdAt
 * @param startedAt
 * @param endedAt
 * @param errorMessage
 */
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

