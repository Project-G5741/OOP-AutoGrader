package com.eiu.capstone.backend.grading.testcase;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Optionally reuses one idle local worker JVM across upload, bulk grade, and lecturer dry-run.
 * Default idle TTL is 0 (close on release) so production RSS drops after OT. Positive TTL parks
 * one alive handle for reuse. Sandbox sessions are never cached (classes are tarred per open).
 * Callers must already hold {@code workerJvmSlot}.
 */
@Component
public class SharedLocalWorkerCache {

    private final long idleTtlMs;
    private final Object lock = new Object();
    private WorkerSessionHandle idle;
    private long idleDeadlineMs;

    public SharedLocalWorkerCache(
            @Value("${app.grading.worker-idle-ttl-ms:0}") long idleTtlMs) {
        this.idleTtlMs = Math.max(0L, idleTtlMs);
    }

    public boolean hasIdleLocal() {
        if (idleTtlMs <= 0) {
            return false;
        }
        synchronized (lock) {
            return idle != null && idle.isAlive() && System.currentTimeMillis() < idleDeadlineMs;
        }
    }

    public WorkerSessionHandle borrow(WorkerSessionFactory factory, Path root, int timeoutSeconds) {
        if (factory.isSandboxEnabled()) {
            return factory.open(root, timeoutSeconds);
        }
        if (idleTtlMs > 0) {
            synchronized (lock) {
                if (idle != null && idle.isAlive() && System.currentTimeMillis() < idleDeadlineMs) {
                    WorkerSessionHandle handle = idle;
                    idle = null;
                    return handle;
                }
                discardIdleLocked();
            }
        }
        return factory.open(root, timeoutSeconds);
    }

    public void release(WorkerSessionFactory factory, WorkerSessionHandle handle) {
        if (handle == null) {
            return;
        }
        if (factory.isSandboxEnabled() || idleTtlMs <= 0) {
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
