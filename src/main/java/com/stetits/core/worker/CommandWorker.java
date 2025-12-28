package com.stetits.core.worker;

import com.stetits.core.persistence.CommandLogsRepository;
import com.stetits.core.persistence.CommandsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class CommandWorker implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(CommandWorker.class);

    private final CommandsRepository commandsRepository;
    private final CommandLogsRepository commandLogsRepository;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    public CommandWorker(CommandsRepository commandsRepository, CommandLogsRepository commandLogsRepository) {
        this.commandsRepository = commandsRepository;
        this.commandLogsRepository = commandLogsRepository;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            workerThread = new Thread(this::loop, "command-worker");
            workerThread.setDaemon(true);
            workerThread.start();
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

                long commandId = claimed.get();
                commandLogsRepository.append(commandId, "INFO", "Command claimed and starting execution");

                // ---- STUB EXECUTION ----
                // Ici, phase suivante: switch type -> orchestration docker
                TimeUnit.SECONDS.sleep(10); // simule traitement

                commandsRepository.markDone(commandId);
                commandLogsRepository.append(commandId, "INFO", "Command completed successfully");

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                // On ne sait pas forcément quel commandId était en cours si exception avant claim,
                // mais si l'exception survient après claim, on peut enrichir en lisant le dernier RUNNING (option).
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

    @Override
    public void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
        log.info("Command worker stopped");
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return 0;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }
}
