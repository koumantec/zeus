package com.stetits.core.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.stetits.core.persistence.CommandLogsRepository;

public class CommandContext {
    private final long commandId;
    private final String stackId;
    private final String type;
    private final JsonNode payload;
    private final CommandLogsRepository commandLogsRepository;

    public CommandContext(long commandId, String stackId, String type, JsonNode payload, CommandLogsRepository commandLogsRepository) {
        this.commandId = commandId;
        this.stackId = stackId;
        this.type = type;
        this.payload = payload;
        this.commandLogsRepository = commandLogsRepository;
    }

    public void debug(String msg) { commandLogsRepository.append(commandId, "DEBUG", msg); }
    public void info(String msg) { commandLogsRepository.append(commandId, "INFO", msg); }
    public void warn(String msg) { commandLogsRepository.append(commandId, "WARN", msg); }
    public void error(String msg) { commandLogsRepository.append(commandId, "ERROR", msg); }

    public long commandId() { return commandId; }
    public String stackId() { return stackId; }
    public String type() { return type; }
    public JsonNode payload() { return payload; }
}
