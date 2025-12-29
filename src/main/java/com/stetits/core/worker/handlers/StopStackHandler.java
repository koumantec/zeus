package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class StopStackHandler implements CommandHandler {
    private final DockerClientFacade docker;
    public StopStackHandler(DockerClientFacade docker) { this.docker = docker; }
    @Override public String type() { return "STOP_STACK"; }

    @Override public void execute(CommandContext ctx) {
        var containers = docker.listByStack(ctx.stackId());
        ctx.info("Stopping " + containers.size() + " containers");
        for (var c : containers) docker.stopContainer(c.id());
    }
}
