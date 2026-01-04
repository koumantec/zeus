package com.stetits.core.api.query;

import com.stetits.core.worker.CommandWorker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WorkerStatusController {
    private final CommandWorker worker;

    public WorkerStatusController(CommandWorker worker) {
        this.worker = worker;
    }

    @GetMapping("/worker")
    public Object status() {
        return Map.of("running", worker.isRunning());
    }
}
