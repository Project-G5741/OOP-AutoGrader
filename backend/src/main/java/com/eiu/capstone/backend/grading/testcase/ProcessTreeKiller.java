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
        destroySnapshot(root);
        try {
            long millis = Math.max(1L, grace.toMillis());
            process.waitFor(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (root.isAlive()) {
            destroySnapshot(root);
            try {
                process.waitFor(grace.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
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
