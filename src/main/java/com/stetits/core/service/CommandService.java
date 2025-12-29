package com.stetits.core.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.persistence.CommandsRepository;
import com.stetits.core.persistence.StacksRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CommandService {

    private final StacksRepository stacksRepository;
    private final CommandsRepository commandsRepository;
    private final ObjectMapper objectMapper;

    public CommandService(StacksRepository stacksRepository, CommandsRepository commandsRepository) {
        this.stacksRepository = stacksRepository;
        this.commandsRepository = commandsRepository;
        this.objectMapper = new ObjectMapper();
    }

    public long enqueueApply(String stackId, String version) {
        ensureStackExists(stackId);
        return enqueue(stackId, "APPLY_STACK_VERSION", Map.of("version", version));
    }

    public long enqueueRollback(String stackId, String targetVersion) {
        ensureStackExists(stackId);
        return enqueue(stackId, "ROLLBACK_STACK", Map.of("targetVersion", targetVersion));
    }

    public long enqueueDelete(String stackId) {
        ensureStackExists(stackId);
        return enqueue(stackId, "DELETE_STACK", Map.of());
    }

    public long enqueueDeploy(String stackId, Map<String, Object> deployPayload) {
        ensureStackExists(stackId);
        return enqueue(stackId, "DEPLOY_APP", deployPayload);
    }

    public long enqueueStart(String stackId) { ensureStackExists(stackId); return enqueue(stackId, "START_STACK", Map.of()); }
    public long enqueueStop(String stackId)  { ensureStackExists(stackId); return enqueue(stackId, "STOP_STACK", Map.of()); }
    public long enqueueRestart(String stackId){ ensureStackExists(stackId); return enqueue(stackId, "RESTART_STACK", Map.of()); }

    public boolean cancel(long commandId) {
        return commandsRepository.cancelIfPending(commandId);
    }

    private long enqueue(String stackId, String type, Map<String, Object> payload) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);
            return commandsRepository.enqueue(stackId, type, payloadJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid payload JSON", e);
        }
    }

    private void ensureStackExists(String stackId) {
        if (stacksRepository.get(stackId).isEmpty()) {
            throw new IllegalArgumentException("Stack not found: " + stackId);
        }
    }
}
