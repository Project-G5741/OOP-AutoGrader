package com.eiu.capstone.backend.grading.testcase.worker;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class BoundedStdout extends OutputStream {

    private final int capBytes;
    private final ByteArrayOutputStream buffer;
    private boolean truncated;

    public BoundedStdout(int capBytes) {
        this.capBytes = Math.max(0, capBytes);
        this.buffer = new ByteArrayOutputStream(Math.min(this.capBytes, 8192));
    }

    @Override
    public synchronized void write(int b) {
        if (buffer.size() >= capBytes) {
            truncated = true;
            return;
        }
        buffer.write(b);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) {
        if (len <= 0) {
            return;
        }
        int room = capBytes - buffer.size();
        if (room <= 0) {
            truncated = true;
            return;
        }
        if (len > room) {
            buffer.write(b, off, room);
            truncated = true;
            return;
        }
        buffer.write(b, off, len);
    }

    public synchronized String text() {
        return buffer.toString(StandardCharsets.UTF_8);
    }

    public synchronized boolean truncated() {
        return truncated;
    }
}
