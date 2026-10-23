package com.eiu.capstone.backend.grading.testcase;

import java.time.Duration;

import com.eiu.capstone.backend.grading.testcase.transport.WorkerTransport;

/**
 * One worker JVM for an upload or dry-run session. Per-session invoke mutex serializes NDJSON.
 */
public final class WorkerSession implements AutoCloseable {

    private final WorkerTransport transport;
    private int respawnCount;

    WorkerSession(WorkerTransport transport) {
        this.transport = transport;
    }

    public Process process() {
        WorkerTransport.ProcessRef ref = transport.processRef();
        return ref == null ? null : ref.process();
    }

    public int stderrBytesKept() {
        WorkerTransport.ProcessRef ref = transport.processRef();
        return ref == null ? 0 : ref.stderrBytesKept().get();
    }

    public int respawnCount() {
        return respawnCount;
    }

    void incrementRespawnCount() {
        respawnCount++;
    }

    public boolean isAlive() {
        return transport.isAlive();
    }

    public void writeLine(String line) {
        transport.writeLine(line);
    }

    public String readLine(Duration timeout, int byteCap) {
        return transport.readLine(timeout, byteCap);
    }

    @Override
    public void close() {
        Process process = process();
        if (process != null) {
            ProcessTreeKiller.kill(process);
        }
        transport.closeTransport();
    }
}
