package com.stetits.core.worker.handlers;

import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ApplyStackVersionHandler implements CommandHandler {

    @Override
    public String type() {
        return "APPLY_STACK_VERSION";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        var version = ctx.payload().path("version").asText(null);
        if (version == null || version.isBlank()) {
            ctx.error("Missing payload.version");
            throw new IllegalArgumentException("payload.version is required");
        }

        ctx.info("Stub APPLY: would converge stack to version=" + version);
        TimeUnit.SECONDS.sleep(5); // simule travail
        ctx.info("Stub APPLY done");
    }
}
