package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.persistence.StacksRepository;
import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class DeleteStackHandler implements CommandHandler {

    private final DockerClientFacade docker;
    private final StacksRepository stacks;

    public DeleteStackHandler(DockerClientFacade docker, StacksRepository stacks) {
        this.docker = docker;
        this.stacks = stacks;
    }

    @Override public String type() { return "DELETE_STACK"; }

    @Override
    public void execute(CommandContext ctx) {
        String stackId = ctx.stackId();
        String networkName = "core_" + stackId;

        // collect volumes from inspect BEFORE removing
        var containers = docker.listByStack(stackId);
        Set<String> volumeNames = new HashSet<>();

        for (var c : containers) {
            try {
                var ins = docker.inspectContainer(c.id());
                for (var m : ins.mounts()) {
                    if (m.volumeName() != null && !m.volumeName().isBlank()) volumeNames.add(m.volumeName());
                }
            } catch (Exception e) {
                ctx.warn("Inspect failed for container " + c.name() + ": " + e.getMessage());
            }
        }

        ctx.info("Deleting containers=" + containers.size());
        for (var c : containers) {
            docker.stopContainer(c.id());
            docker.removeContainer(c.id(), true);
            ctx.info("Removed container " + c.name());
        }

        // remove network
        docker.findNetworkByName(networkName).ifPresent(n -> {
            ctx.info("Removing network " + n.name());
            docker.removeNetwork(n.id());
        });

        // remove volumes (after containers)
        ctx.info("Removing volumes=" + volumeNames.size());
        for (String v : volumeNames) {
            try {
                docker.removeVolume(v);
                ctx.info("Removed volume " + v);
            } catch (Exception e) {
                ctx.warn("Volume remove failed " + v + ": " + e.getMessage());
            }
        }

        stacks.setCurrentVersion(stackId, null);
        ctx.info("Stack current_version cleared");
    }
}
