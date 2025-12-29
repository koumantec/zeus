package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class RestartStackHandler implements CommandHandler {
    private final DockerClientFacade docker;
    public RestartStackHandler(DockerClientFacade docker) { this.docker = docker; }
    @Override public String type() { return "RESTART_STACK"; }

    @Override public void execute(CommandContext ctx) {
        var containers = docker.listByStack(ctx.stackId());
        ctx.info("Restarting " + containers.size() + " containers");
        for (var c : containers) docker.restartContainer(c.id());
    }
}
