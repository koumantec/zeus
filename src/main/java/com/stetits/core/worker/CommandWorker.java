package com.stetits.core.worker;

import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix = "orchestrator.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommandWorker implements SmartLifecycle {
    private static final Logger log = LoggerFactory.getLogger(CommandWorker.class);
    @Value("${orchestrator.worker.autostart:true}")
    boolean autoStart;
    private final CommandsRepository commandsRepository;
    private final CommandLogsRepository commandLogsRepository;
    private final CommandExecutionService executor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread thread;

    public CommandWorker(CommandsRepository commandsRepository, CommandLogsRepository commandLogsRepository, CommandExecutionService executor) {
        this.commandsRepository = commandsRepository;
        this.commandLogsRepository = commandLogsRepository;
        this.executor = executor;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            thread = new Thread(this::loop, "command-worker");
            thread.setDaemon(true);
            thread.start();
            log.info("Command worker started");
        }
    }

    private void loop() {
        while (running.get()) {
            try {
                if (!processOne()) TimeUnit.MILLISECONDS.sleep(250);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.error("Worker loop error", e);
                try {
                    TimeUnit.MILLISECONDS.sleep(500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    // Dans CommandWorker
    public boolean processOne() throws InterruptedException {
        var claimed = commandsRepository.claimNextPending();
        if (claimed.isEmpty()) return false;

        long cmdId = claimed.get();
        commandLogsRepository.append(cmdId, "INFO", "Claimed command; entering execution");

        try {
            executor.execute(cmdId);
            commandsRepository.markDone(cmdId);
            commandLogsRepository.append(cmdId, "INFO", "Command marked DONE");
        } catch (Exception e) {
            String msg = e.getMessage() == null ? e.getClass().getName() : e.getMessage();
            commandsRepository.markFailed(cmdId, msg);
            commandLogsRepository.append(cmdId, "ERROR", "Command marked FAILED: " + msg);
        }
        return true;
    }

    private static String stacktrace(Throwable t, int maxChars) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String s = sw.toString();
        return s.length() <= maxChars ? s : s.substring(0, maxChars) + "\n...truncated...";
    }

    @Override
    public void stop() {
        running.set(false);
        if (thread != null) thread.interrupt();
        log.info("Command worker stopped");
    }

    @Override public boolean isRunning() { return running.get(); }
    @Override public void stop(Runnable callback) { stop(); callback.run(); }
    @Override
    public boolean isAutoStartup() {
        return autoStart;
    }
    @Override public int getPhase() { return 0; }
}
