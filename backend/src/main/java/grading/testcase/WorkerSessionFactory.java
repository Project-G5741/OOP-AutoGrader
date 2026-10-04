package com.eiu.capstone.backend.grading.testcase;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WorkerSessionFactory {

    private final boolean sandboxEnabled;
    private final WorkerProcessClient workerProcessClient;
    private final RemoteWorkerSessionClient remoteWorkerSessionClient;

    public WorkerSessionFactory(
            @Value("${app.grading.sandbox.enabled:false}") boolean sandboxEnabled,
            WorkerProcessClient workerProcessClient,
            RemoteWorkerSessionClient remoteWorkerSessionClient) {
        this.sandboxEnabled = sandboxEnabled;
        this.workerProcessClient = workerProcessClient;
        this.remoteWorkerSessionClient = remoteWorkerSessionClient;
    }

    public WorkerSessionHandle open(Path submissionOrCompileRoot, int timeoutSeconds) {
        if (sandboxEnabled) {
            if (submissionOrCompileRoot == null) {
                return WorkerSessionHandle.failedRemote(SandboxInfraErrors.STUDENT_MESSAGE, 0);
            }
            return remoteWorkerSessionClient.open(submissionOrCompileRoot, timeoutSeconds);
        }
        return WorkerSessionHandle.start(workerProcessClient, timeoutSeconds);
    }

    public boolean isSandboxEnabled() {
        return sandboxEnabled;
    }
}
