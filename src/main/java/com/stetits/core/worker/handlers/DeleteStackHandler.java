package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.worker.CommandContext;
import com.stetits.core.worker.CommandHandler;
import org.springframework.stereotype.Component;

@Component
public class DeleteStackHandler implements CommandHandler {
    private final DockerClientFacade docker;
    private final StacksRepository stacks;

    public DeleteStackHandler(DockerClientFacade docker, StacksRepository stacks) {
        this.docker = docker; this.stacks = stacks;
    }

    @Override public String type() { return "DELETE_STACK"; }

    @Override
    public void execute(CommandContext ctx) {
        var containers = docker.listByStack(ctx.stackId());
        ctx.info("Deleting stack containers=" + containers.size());

        for (var c : containers) {
            docker.stopContainer(c.id());
            docker.removeContainer(c.id(), true);
            ctx.info("Removed container " + c.name());
        }
        stacks.setCurrentVersion(ctx.stackId(), null);
        ctx.info("Stack current_version cleared");
    }
}

