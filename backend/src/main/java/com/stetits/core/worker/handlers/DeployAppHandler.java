package com.stetits.core.worker.handlers;

import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class DeployAppHandler implements CommandHandler {

    @Override
    public String type() {
        return "DEPLOY_APP";
    }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        var targetService = ctx.payload().path("targetService").asText("");
        ctx.info("Stub DEPLOY: targetService=" + targetService + " payload=" + ctx.payload().toString());
        TimeUnit.SECONDS.sleep(5); // simule travail
        ctx.info("Stub DEPLOY done");
    }
}

