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

        var orderOpt = resolver.resolveOrders(stackId);
        if (orderOpt.isEmpty()) {
            ctx.warn("No current_version/spec found. Starting all containers without dependency ordering.");
            for (var c : containers) docker.startContainer(c.id());
            return;
        }

        var order = orderOpt.get().startOrder();
        ctx.info("Starting in order: " + order);

        for (String svc : order) {
            var c = byName.get(svc);
            if (c != null) docker.startContainer(c.id());
        }

        // start ceux qui restent (drift)
        Set<String> started = new HashSet<>(order.stream().toList());
        for (var c : containers) {
            if (!started.contains(c.name())) docker.startContainer(c.id());
        }
    }
}
