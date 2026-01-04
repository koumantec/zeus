package com.stetits.core.worker.handlers;

import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RollbackStackHandler implements CommandHandler {

    @Override
    public String type() {
        return "ROLLBACK_STACK";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        var target = ctx.payload().path("targetVersion").asText(null);
        if (target == null || target.isBlank()) {
            ctx.error("Missing payload.targetVersion");
            throw new IllegalArgumentException("payload.targetVersion is required");
        }

        ctx.info("Stub ROLLBACK: would converge stack to targetVersion=" + target);
        TimeUnit.SECONDS.sleep(5); // simule travail
        ctx.info("Stub ROLLBACK done");
    }
}
