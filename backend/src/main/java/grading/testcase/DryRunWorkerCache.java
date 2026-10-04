package com.eiu.capstone.backend.grading.testcase;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Reuses one local worker JVM across lecturer dry-runs so repeated Run clicks
 * skip cold spawn. Sandbox sessions are never cached (classes are tarred per open).
 */
@Component
public class DryRunWorkerCache {

    private static final long IDLE_TTL_MS = 60_000L;

    private final Object lock = new Object();
    private WorkerSessionHandle idle;
    private long idleDeadlineMs;

    public boolean hasIdleLocal() {
        synchronized (lock) {
            return idle != null && idle.isAlive() && System.currentTimeMillis() < idleDeadlineMs;
        }
    }

    public WorkerSessionHandle borrow(WorkerSessionFactory factory, Path root, int timeoutSeconds) {
        if (factory.isSandboxEnabled()) {
            return factory.open(root, timeoutSeconds);
        }
        synchronized (lock) {
            if (idle != null && idle.isAlive() && System.currentTimeMillis() < idleDeadlineMs) {
                WorkerSessionHandle handle = idle;
                idle = null;
                return handle;
            }
            discardIdleLocked();
        }
        return factory.open(root, timeoutSeconds);
    }

    public void release(WorkerSessionFactory factory, WorkerSessionHandle handle) {
        if (handle == null) {
            return;
        }
        if (factory.isSandboxEnabled()) {
            handle.close();
            return;
        }
        synchronized (lock) {
            if (!handle.isAlive()) {
                handle.close();
                return;
            }
            discardIdleLocked();
            idle = handle;
            idleDeadlineMs = System.currentTimeMillis() + IDLE_TTL_MS;
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (lock) {
            discardIdleLocked();
        }
    }

    private void discardIdleLocked() {
        if (idle != null) {
            idle.close();
            idle = null;
        }
    }
}
