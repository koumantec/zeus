package com.stetits.core.worker.handlers;

import com.stetits.core.docker.DockerClientFacade;
import com.stetits.core.stack.StackSpecResolver;
import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class StartStackHandler implements CommandHandler {

    private final DockerClientFacade docker;
    private final StackSpecResolver resolver;

    public StartStackHandler(DockerClientFacade docker, StackSpecResolver resolver) {
        this.docker = docker;
        this.resolver = resolver;
    }

    @Override public String type() { return "START_STACK"; }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        String stackId = ctx.stackId();
        var containers = docker.listByStack(stackId);

        Map<String, DockerClientFacade.ContainerInfo> byName = new HashMap<>();
        for (var c : containers) byName.put(c.name(), c);

        var ord = resolver.resolve(stackId);
        if (ord.isEmpty()) {
            ctx.warn("No spec; starting all containers without ordering");
            for (var c : containers) docker.startContainer(c.id());
            return;
        }

        ctx.info("Starting in order: " + ord.get().startOrder());
        Set<String> touched = new HashSet<>();
        for (String svc : ord.get().startOrder()) {
            String name = "core_" + stackId + "_" + svc;
            var c = byName.get(name);
            if (c != null) {
                docker.startContainer(c.id());
                touched.add(name);
            }
        }
        for (var c : containers) if (!touched.contains(c.name())) docker.startContainer(c.id());
    }
}
