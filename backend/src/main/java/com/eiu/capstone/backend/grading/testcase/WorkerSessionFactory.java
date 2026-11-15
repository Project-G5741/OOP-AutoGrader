package com.eiu.capstone.backend.grading.testcase;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

@Component
public class WorkerSessionFactory {

    private final WorkerProcessClient workerProcessClient;

    public WorkerSessionFactory(WorkerProcessClient workerProcessClient) {
        this.workerProcessClient = workerProcessClient;
    }

    public WorkerSessionHandle open(Path submissionOrCompileRoot, int timeoutSeconds) {
        return WorkerSessionHandle.start(workerProcessClient, timeoutSeconds);
    }

    /** Operational tests always use the local worker. The container runner is gone. */
    public boolean isSandboxEnabled() {
        return false;
    }
}
