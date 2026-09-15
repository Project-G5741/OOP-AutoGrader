package com.eiu.capstone.backend.grading.testcase.transport;

import java.time.Duration;

import com.eiu.capstone.backend.grading.testcase.WorkerSpawnException;
import com.eiu.capstone.backend.grading.testcase.WorkerTimeoutException;

public interface WorkerTransport {

    void writeLine(String line);

    String readLine(Duration timeout, int byteCap);

    boolean isAlive();

    void closeTransport();

    default ProcessRef processRef() {
        return null;
    }

    record ProcessRef(java.lang.Process process, Thread stderrDrain, java.util.concurrent.atomic.AtomicInteger stderrBytesKept) {}
}
