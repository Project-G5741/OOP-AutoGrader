package com.eiu.capstone.backend.grading.testcase;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One worker JVM for an upload or dry-run session. Per-session invoke mutex serializes NDJSON.
 */
public final class WorkerSession implements AutoCloseable {

    private final Process process;
    private final BufferedWriter ipcStdin;
    private final InputStream ipcStdout;
    private final Thread stderrDrain;
    private final AtomicInteger stderrBytesKept;
    private final Object invokeMutex = new Object();
    private int respawnCount;

    WorkerSession(Process process,
                  Thread stderrDrain,
                  AtomicInteger stderrBytesKept) {
        this.process = process;
        this.ipcStdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.ipcStdout = process.getInputStream();
        this.stderrDrain = stderrDrain;
        this.stderrBytesKept = stderrBytesKept;
    }

    public Process process() {
        return process;
    }

    public int stderrBytesKept() {
        return stderrBytesKept.get();
    }

    public int respawnCount() {
        return respawnCount;
    }

    void incrementRespawnCount() {
        respawnCount++;
    }

    public boolean isAlive() {
        return process.isAlive();
    }

    public void writeLine(String line) {
        synchronized (invokeMutex) {
            try {
                ipcStdin.write(line);
                ipcStdin.write('\n');
                ipcStdin.flush();
            } catch (Exception e) {
                throw new WorkerSpawnException("Failed to write worker IPC line", e);
            }
        }
    }

    public String readLine(Duration timeout, int byteCap) {
        long deadline = System.nanoTime() + timeout.toNanos();
        ByteArrayOutputStream line = new ByteArrayOutputStream();
        try {
            while (System.nanoTime() < deadline) {
                if (ipcStdout.available() <= 0 && process.isAlive()) {
                    TimeUnit.MILLISECONDS.sleep(10);
                    continue;
                }
                int next = ipcStdout.read();
                if (next < 0) {
                    return line.size() == 0 ? null : decodeIpcLine(line.toByteArray());
                }
                if (next == '\n') {
                    return decodeIpcLine(line.toByteArray());
                }
                if (line.size() >= byteCap) {
                    throw new WorkerSpawnException("Worker IPC line exceeded " + byteCap + " bytes");
                }
                line.write(next);
            }
            throw new WorkerTimeoutException("Timed out reading worker IPC line");
        } catch (WorkerSpawnException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new WorkerSpawnException("Interrupted reading worker IPC line", e);
        } catch (Exception e) {
            throw new WorkerSpawnException("Failed to read worker IPC line", e);
        }
    }

    private static String decodeIpcLine(byte[] bytes) {
        int length = bytes.length;
        if (length > 0 && bytes[length - 1] == '\r') {
            length--;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        ProcessTreeKiller.kill(process);
        if (stderrDrain != null) {
            stderrDrain.interrupt();
        }
        try {
            ipcStdin.close();
        } catch (Exception ignored) {
            // closing a killed process
        }
    }
}
