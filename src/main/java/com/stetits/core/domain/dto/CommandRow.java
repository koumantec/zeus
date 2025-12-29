package com.stetits.core.domain.dto;

/**
 * On garde CommandDto pour les APIs READ, mais l’exécuteur travaille avec CommandRow minimal.
 * @param id
 * @param stackId
 * @param type
 * @param payloadJson
 * @param status
 */
public record CommandRow(
        long id,
        String stackId,
        String type,
        String payloadJson,
        String status
) {}
