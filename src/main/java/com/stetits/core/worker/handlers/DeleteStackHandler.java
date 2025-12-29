package com.stetits.core.worker.handlers;

import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DeleteStackHandler implements CommandHandler {

    @Override
    public String type() {
        return "DELETE_STACK";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        ctx.info("Stub DELETE: would stop/remove containers and mark stack deleted");
        TimeUnit.SECONDS.sleep(5); // simule travail
        ctx.info("Stub DELETE done");
    }
}
