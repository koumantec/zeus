package com.stetits.core.worker;

import com.stetits.core.repository.CommandLogsRepository;
import com.stetits.core.repository.CommandsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CommandWorker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CommandWorker.class);

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
                Optional<Long> claimed = commandsRepository.claimNextPending();
                if (claimed.isEmpty()) {
                    TimeUnit.MILLISECONDS.sleep(250);
                    continue;
                }

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
                    commandLogsRepository.append(cmdId, "ERROR", "Stacktrace:\n" + stacktrace(e, 8000));
                }

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
    @Override public int getPhase() { return 0; }
}
