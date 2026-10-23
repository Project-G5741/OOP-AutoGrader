package com.eiu.capstone.backend.grading.testcase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Best-effort process-tree kill: descendants deepest-first, then the root, then a short wait.
 */
public final class ProcessTreeKiller {

    private ProcessTreeKiller() {}

    public static void kill(Process process) {
        kill(process, Duration.ofSeconds(2));
    }

    public static void kill(Process process, Duration grace) {
        if (process == null || process.toHandle() == null) {
            return;
        }
        ProcessHandle root = process.toHandle();
        if (!root.isAlive()) {
            try {
                process.waitFor(1, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return;
        }
        destroySnapshot(root);
        waitUntilDead(process, root, grace);
        if (root.isAlive()) {
            destroySnapshot(root);
            waitUntilDead(process, root, grace);
        }
    }

    private static void waitUntilDead(Process process, ProcessHandle root, Duration grace) {
        if (!root.isAlive()) {
            return;
        }
        long remaining = Math.max(1L, grace.toMillis());
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(remaining);
        while (root.isAlive() && System.nanoTime() < deadline) {
            long slice = Math.min(50L, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()));
            if (slice <= 0) {
                break;
            }
            try {
                if (process.waitFor(slice, TimeUnit.MILLISECONDS)) {
                    return;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private static void destroySnapshot(ProcessHandle root) {
        List<ProcessHandle> descendants = new ArrayList<>(root.descendants().toList());
        for (int i = descendants.size() - 1; i >= 0; i--) {
            descendants.get(i).destroyForcibly();
        }
        root.destroyForcibly();
    }
}
