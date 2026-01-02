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

        var orderOpt = resolver.resolveOrders(stackId);
        if (orderOpt.isEmpty()) {
            ctx.warn("No current_version/spec found. Stopping all containers without dependency ordering.");
            for (var c : containers) docker.stopContainer(c.id());
            return;
        }

        var order = orderOpt.get().stopOrder();
        ctx.info("Stopping in order: " + order);

        for (String svc : order) {
            var c = byName.get(svc);
            if (c != null) docker.stopContainer(c.id());
        }

        // sécurité: stop tout container restant (si service supprimé du spec mais encore présent)
        Set<String> stopped = new HashSet<>(order.stream().toList());
        for (var c : containers) {
            if (!stopped.contains(c.name())) docker.stopContainer(c.id());
        }
    }
}
