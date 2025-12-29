package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class StartStackHandler implements CommandHandler {
    private final DockerClientFacade docker;
    public StartStackHandler(DockerClientFacade docker) { this.docker = docker; }
    @Override public String type() { return "START_STACK"; }

    @Override public void execute(CommandContext ctx) {
        var containers = docker.listByStack(ctx.stackId());
        ctx.info("Starting " + containers.size() + " containers");
        for (var c : containers) docker.startContainer(c.id());
    }
}
