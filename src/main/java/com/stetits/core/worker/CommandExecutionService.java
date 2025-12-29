package com.stetits.core.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CommandExecutionService {

    private final CommandsRepository commandsRepository;
    private final CommandLogsRepository commandLogsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CommandHandler> handlers;

    public CommandExecutionService(
            CommandsRepository commandsRepository,
            CommandLogsRepository commandLogsRepository,
            List<CommandHandler> handlers
    ) {
        this.commandsRepository = commandsRepository;
        this.commandLogsRepository = commandLogsRepository;
        this.handlers = handlers.stream().collect(Collectors.toMap(CommandHandler::type, h -> h));
    }

    public void execute(long commandId) throws Exception {
        var rowOpt = commandsRepository.getRow(commandId);
        if (rowOpt.isEmpty()) {
            return; // commande supprimée ou inexistante (peu probable)
        }

        var commandRow = rowOpt.get();

        // Sécurité : on n’exécute que si RUNNING
        if (!"RUNNING".equals(commandRow.status())) {
            return;
        }

        JsonNode payload = parsePayload(commandRow.payloadJson());
        var ctx = new CommandContext(commandRow.id(), commandRow.stackId(), commandRow.type(), payload, commandLogsRepository);

        ctx.info("Executing command type=" + commandRow.type() + " stackId=" + commandRow.stackId());

        CommandHandler handler = handlers.get(commandRow.type());
        if (handler == null) {
            ctx.error("No handler registered for command type: " + commandRow.type());
            throw new IllegalStateException("No handler for type=" + commandRow.type());
        }

        handler.execute(ctx);

        ctx.info("Execution finished (handler completed)");
    }

    private JsonNode parsePayload(String payloadJson) throws Exception {
        if (payloadJson == null || payloadJson.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(payloadJson);
    }
}
