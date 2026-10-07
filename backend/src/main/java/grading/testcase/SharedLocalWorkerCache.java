package com.eiu.capstone.backend.grading.testcase;

import java.nio.file.Path;

import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Reuses one idle local worker JVM across upload, bulk grade, and lecturer dry-run
 * so consecutive calls skip cold spawn. Sandbox sessions are never cached (classes
 * are tarred per open). Callers must already hold {@code workerJvmSlot}.
 */
@Component
public class SharedLocalWorkerCache {

    private static final long DEFAULT_IDLE_TTL_MS = 60_000L;

    private final long idleTtlMs;
    private final Object lock = new Object();
    private WorkerSessionHandle idle;
    private long idleDeadlineMs;

    public SharedLocalWorkerCache() {
        this(DEFAULT_IDLE_TTL_MS);
    }

    /** Visible for unit tests with a short TTL. */
    public SharedLocalWorkerCache(long idleTtlMs) {
        this.idleTtlMs = idleTtlMs <= 0 ? DEFAULT_IDLE_TTL_MS : idleTtlMs;
    }

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
            idleDeadlineMs = System.currentTimeMillis() + idleTtlMs;
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
