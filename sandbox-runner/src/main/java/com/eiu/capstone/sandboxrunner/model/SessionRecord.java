package com.eiu.capstone.sandboxrunner.model;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SessionRecord implements AutoCloseable {

    private static final int IPC_LINE_CAP_BYTES = 524_288;

    private final String sessionId;
    private final String containerId;
    private final Instant createdAt;
    private final Process workerProcess;
    private final BufferedWriter stdin;
    private final BufferedInputStream stdout;
    private final Thread stderrDrain;
    private final AtomicInteger stderrBytesKept = new AtomicInteger();
    private final Object invokeMutex = new Object();

    public SessionRecord(String sessionId,
                         String containerId,
                         Process workerProcess) {
        this.sessionId = sessionId;
        this.containerId = containerId;
        this.createdAt = Instant.now();
        this.workerProcess = workerProcess;
        this.stdin = new BufferedWriter(new OutputStreamWriter(workerProcess.getOutputStream(), StandardCharsets.UTF_8));
        this.stdout = new BufferedInputStream(workerProcess.getInputStream());
        this.stderrDrain = new Thread(this::drainStderr, "sandbox-worker-stderr-" + sessionId);
        this.stderrDrain.setDaemon(true);
        this.stderrDrain.start();
    }

    public String containerId() {
        return containerId;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public boolean isAlive() {
        return workerProcess.isAlive();
    }

    public String invokeLine(String requestLine, Duration timeout) throws Exception {
        synchronized (invokeMutex) {
            if (!isAlive()) {
                throw new IllegalStateException("Worker stopped");
            }
            stdin.write(requestLine);
            stdin.write('\n');
            stdin.flush();
            return readLine(timeout);
        }
    }

    private String readLine(Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        java.io.ByteArrayOutputStream line = new java.io.ByteArrayOutputStream();
        while (System.nanoTime() < deadline) {
            if (stdout.available() <= 0 && workerProcess.isAlive()) {
                TimeUnit.MILLISECONDS.sleep(10);
                continue;
            }
            int next = stdout.read();
            if (next < 0) {
                return line.size() == 0 ? null : decodeLine(line.toByteArray());
            }
            if (next == '\n') {
                return decodeLine(line.toByteArray());
            }
            if (line.size() >= IPC_LINE_CAP_BYTES) {
                throw new IllegalStateException("IPC line exceeded cap");
            }
            line.write(next);
        }
        throw new java.util.concurrent.TimeoutException("Timed out reading worker IPC line");
    }

    private static String decodeLine(byte[] bytes) {
        int length = bytes.length;
        if (length > 0 && bytes[length - 1] == '\r') {
            length--;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    private void drainStderr() {
        byte[] buffer = new byte[4096];
        try (InputStream err = workerProcess.getErrorStream()) {
            int read;
            while ((read = err.read(buffer)) != -1) {
                int already = stderrBytesKept.get();
                int room = Math.max(0, 65536 - already);
                if (room > 0) {
                    stderrBytesKept.addAndGet(Math.min(room, read));
                }
            }
        } catch (Exception ignored) {
            // process closed
        }
    }

    @Override
    public void close() {
        if (workerProcess.isAlive()) {
            workerProcess.destroyForcibly();
        }
        stderrDrain.interrupt();
        try {
            stdin.close();
        } catch (Exception ignored) {
            // closing killed process
        }
    }
}
