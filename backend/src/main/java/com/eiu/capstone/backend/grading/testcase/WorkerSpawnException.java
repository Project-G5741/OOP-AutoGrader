package com.eiu.capstone.backend.grading.testcase;

public class WorkerSpawnException extends RuntimeException {

    public WorkerSpawnException(String message) {
        super(message);
    }

    public WorkerSpawnException(String message, Throwable cause) {
        super(message, cause);
    }
}
