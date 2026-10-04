package com.eiu.capstone.backend.grading.testcase;

public class WorkerTimeoutException extends WorkerSpawnException {

    public WorkerTimeoutException(String message) {
        super(message);
    }
}
