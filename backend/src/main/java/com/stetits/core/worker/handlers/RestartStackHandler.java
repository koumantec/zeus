package com.stetits.core.worker.handlers;

import com.stetits.core.worker.CommandHandler;
import com.stetits.core.worker.CommandContext;
import org.springframework.stereotype.Component;

@Component
public class RestartStackHandler implements CommandHandler {

    private final StopStackHandler stop;
    private final StartStackHandler start;

    public RestartStackHandler(StopStackHandler stop, StartStackHandler start) {
        this.stop = stop;
        this.start = start;
    }

    @Override public String type() { return "RESTART_STACK"; }

    @Override
    public void execute(CommandContext ctx) throws Exception {
        ctx.info("Restart: stop then start");
        stop.execute(ctx);
        start.execute(ctx);
    }
}
