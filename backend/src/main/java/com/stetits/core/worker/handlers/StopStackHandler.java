package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.stack.StackSpecResolver;
import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StopStackHandler implements CommandHandler {

    private final DockerClientFacade docker;
    private final StackSpecResolver resolver;

    public StopStackHandler(DockerClientFacade docker, StackSpecResolver resolver) {
        this.docker = docker;
        this.resolver = resolver;
    }

    @Override public String type() { return "STOP_STACK"; }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        String stackId = ctx.stackId();
        var containers = docker.listByStack(stackId);

        Map<String, DockerClientFacade.ContainerInfo> byName = new HashMap<>();
        for (var c : containers) byName.put(c.name(), c);

        var ord = resolver.resolve(stackId);
        if (ord.isEmpty()) {
            ctx.warn("No spec; stopping all containers without ordering");
            for (var c : containers) docker.stopContainer(c.id());
            return;
        }

        ctx.info("Stopping in order: " + ord.get().stopOrder());
        Set<String> touched = new HashSet<>();
        for (String svc : ord.get().stopOrder()) {
            String name = "core_" + stackId + "_" + svc;
            var c = byName.get(name);
            if (c != null) {
                docker.stopContainer(c.id());
                touched.add(name);
            }
        }
        for (var c : containers) if (!touched.contains(c.name())) docker.stopContainer(c.id());
    }
}
