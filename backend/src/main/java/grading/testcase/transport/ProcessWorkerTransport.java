package com.eiu.capstone.backend.grading.testcase.transport;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.eiu.capstone.backend.grading.testcase.WorkerSpawnException;
import com.eiu.capstone.backend.grading.testcase.WorkerTimeoutException;

public final class ProcessWorkerTransport implements WorkerTransport {

    private static final int READ_CHUNK = 16384;

    private final Process process;
    private final BufferedWriter ipcStdin;
    private final InputStream ipcStdout;
    private final Thread stderrDrain;
    private final AtomicInteger stderrBytesKept;
    private final Object invokeMutex = new Object();
    private final byte[] readBuf = new byte[READ_CHUNK];
    /** Bytes already pulled from the stream but not yet returned as a complete line. */
    private final ByteArrayOutputStream carry = new ByteArrayOutputStream();

    public ProcessWorkerTransport(Process process, Thread stderrDrain, AtomicInteger stderrBytesKept) {
        this.process = process;
        this.ipcStdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
        this.ipcStdout = process.getInputStream();
        this.stderrDrain = stderrDrain;
        this.stderrBytesKept = stderrBytesKept;
    }

    @Override
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

    @Override
    public String readLine(Duration timeout, int byteCap) {
        synchronized (invokeMutex) {
            long deadline = System.nanoTime() + timeout.toNanos();
            try {
                String fromCarry = takeCompleteLine(byteCap);
                if (fromCarry != null) {
                    return fromCarry;
                }
                while (System.nanoTime() < deadline) {
                    int available = ipcStdout.available();
                    if (available <= 0) {
                        if (!process.isAlive()) {
                            // Drain any last buffered bytes after exit.
                            int n = ipcStdout.read(readBuf, 0, readBuf.length);
                            if (n > 0) {
                                appendBytes(readBuf, n, byteCap);
                                String line = takeCompleteLine(byteCap);
                                if (line != null) {
                                    return line;
                                }
                                continue;
                            }
                            return carry.size() == 0 ? null : finishCarryLine();
                        }
                        TimeUnit.MILLISECONDS.sleep(1);
                        continue;
                    }
                    int toRead = Math.min(available, readBuf.length);
                    int n = ipcStdout.read(readBuf, 0, toRead);
                    if (n < 0) {
                        return carry.size() == 0 ? null : finishCarryLine();
                    }
                    appendBytes(readBuf, n, byteCap);
                    String line = takeCompleteLine(byteCap);
                    if (line != null) {
                        return line;
                    }
                }
                throw new WorkerTimeoutException("Timed out reading worker IPC line");
            } catch (WorkerTimeoutException e) {
                throw e;
            } catch (WorkerSpawnException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WorkerSpawnException("Interrupted reading worker IPC line", e);
            } catch (Exception e) {
                throw new WorkerSpawnException("Failed to read worker IPC line", e);
            }
        }
    }

    private void appendBytes(byte[] buf, int n, int byteCap) {
        if (carry.size() + n > byteCap) {
            throw new WorkerSpawnException("Worker IPC line exceeded " + byteCap + " bytes");
        }
        carry.write(buf, 0, n);
    }

    /** If carry contains a full line (through {@code \n}), return it and keep the rest. */
    private String takeCompleteLine(int byteCap) {
        byte[] all = carry.toByteArray();
        for (int i = 0; i < all.length; i++) {
            if (all[i] == '\n') {
                String line = decodeIpcLine(all, i);
                carry.reset();
                if (i + 1 < all.length) {
                    carry.write(all, i + 1, all.length - (i + 1));
                }
                if (line.getBytes(StandardCharsets.UTF_8).length > byteCap) {
                    throw new WorkerSpawnException("Worker IPC line exceeded " + byteCap + " bytes");
                }
                return line;
            }
        }
        return null;
    }

    private String finishCarryLine() {
        byte[] all = carry.toByteArray();
        carry.reset();
        return decodeIpcLine(all, all.length);
    }

    @Override
    public boolean isAlive() {
        return process.isAlive();
    }

    @Override
    public void closeTransport() {
        if (stderrDrain != null) {
            stderrDrain.interrupt();
        }
        try {
            ipcStdin.close();
        } catch (Exception ignored) {
            // closing killed process
        }
    }

    @Override
    public ProcessRef processRef() {
        return new ProcessRef(process, stderrDrain, stderrBytesKept);
    }

    private static String decodeIpcLine(byte[] bytes, int length) {
        if (length > 0 && bytes[length - 1] == '\r') {
            length--;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }
}
