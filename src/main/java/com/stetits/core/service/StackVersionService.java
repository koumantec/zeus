package com.stetits.core.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.persistence.JdbcStackVersionsRepository;
import com.stetits.core.persistence.StacksRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class StackVersionService {

    private final StacksRepository stacks;
    private final JdbcStackVersionsRepository versions;
    private final ObjectMapper om = new ObjectMapper();

    public StackVersionService(StacksRepository stacks, JdbcStackVersionsRepository versions) {
        this.stacks = stacks;
        this.versions = versions;
    }

    public record CreateVersionRequest(
            String version,     // optionnel
            JsonNode body,      // requis
            String createdBy,   // optionnel (MVP)
            String comment      // optionnel
    ) {}

    public record CreateVersionResponse(
            String stackId,
            String version,
            String parentVersion,
            String bodySha256
    ) {}

    public CreateVersionResponse create(String stackId, CreateVersionRequest req) throws Exception {
        if (stacks.get(stackId).isEmpty()) {
            throw new IllegalArgumentException("Stack not found: " + stackId);
        }
        if (req.body == null || req.body.isNull()) {
            throw new IllegalArgumentException("body is required");
        }

        // Canonical JSON -> hash stable
        String canonicalBody = om.writeValueAsString(req.body);
        String sha = sha256Hex(canonicalBody);

        Optional<String> latest = versions.findLatestVersion(stackId);
        Optional<String> latestSha = versions.findLatestBodySha(stackId);

        // Option : éviter de créer une version identique (anti-bruit)
        if (latestSha.isPresent() && latestSha.get().equals(sha)) {
            // on renvoie la version existante sans en créer une nouvelle
            return new CreateVersionResponse(stackId, latest.orElse("v0"), versions.findLatestParentVersion(stackId).orElse(null), sha);
        }

        String parent = latest.orElse(null);
        String version = (req.version == null || req.version.isBlank())
                ? "v" + Instant.now().toEpochMilli()
                : req.version;

        versions.insert(new com.stetits.core.persistence.StackVersionsRepository.StackVersionRow(
                stackId,
                version,
                parent,
                canonicalBody,
                sha,
                req.createdBy == null ? "system" : req.createdBy,
                req.comment
        ));

        return new CreateVersionResponse(stackId, version, parent, sha);
    }

    public static String sha256Hex(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(dig);
    }
}
