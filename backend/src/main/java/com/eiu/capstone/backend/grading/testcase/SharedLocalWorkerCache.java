package com.eiu.capstone.backend.grading.testcase;

import java.nio.file.Path;
import java.util.concurrent.Semaphore;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

/**
 * Optionally reuses one idle local worker JVM across upload, bulk grade, and lecturer dry-run.
 * Default idle TTL is a short clean peek ({@code app.grading.worker-idle-ttl-ms}, typically 5s).
 * Clean release parks when another waiter is queued on {@code workerJvmSlot} or when TTL &gt; 0.
 * Dirty (hang/abort) and sandbox sessions are never parked. Callers must already hold the slot.
 */
@Component
public class SharedLocalWorkerCache {

    private final long idleTtlMs;
    private final Semaphore workerJvmSlot;
    private final Object lock = new Object();
    private WorkerSessionHandle idle;
    private long idleDeadlineMs;

    @Autowired
    public SharedLocalWorkerCache(
            @Value("${app.grading.worker-idle-ttl-ms:5000}") long idleTtlMs,
            @Qualifier("workerJvmSlot") Semaphore workerJvmSlot) {
        this.idleTtlMs = Math.max(0L, idleTtlMs);
        this.workerJvmSlot = workerJvmSlot;
    }

    /** Test helper without a slot (demand park disabled). */
    public SharedLocalWorkerCache(long idleTtlMs) {
        this(idleTtlMs, null);
    }

    public boolean hasIdleLocal() {
        synchronized (lock) {
            return idle != null && idle.isAlive() && !idle.isDirty() && System.currentTimeMillis() < idleDeadlineMs;
        }
    }

    public WorkerSessionHandle borrow(WorkerSessionFactory factory, Path root, int timeoutSeconds) {
        if (factory.isSandboxEnabled()) {
            return factory.open(root, timeoutSeconds);
        }
        synchronized (lock) {
            if (idle != null && idle.isAlive() && !idle.isDirty() && System.currentTimeMillis() < idleDeadlineMs) {
                WorkerSessionHandle handle = idle;
                idle = null;
                return handle;
            }
            discardIdleLocked();
        }
        return factory.open(root, timeoutSeconds);
    }

    public void release(WorkerSessionFactory factory, WorkerSessionHandle handle) {
        release(factory, handle, workerJvmSlot);
    }

    public void release(WorkerSessionFactory factory, WorkerSessionHandle handle, Semaphore slot) {
        if (handle == null) {
            return;
        }
        if (factory.isSandboxEnabled() || handle.isDirty() || !handle.isAlive()) {
            handle.close();
            return;
        }
        boolean demand = slot != null && slot.hasQueuedThreads();
        if (!demand && idleTtlMs <= 0) {
            handle.close();
            return;
        }
        long parkTtlMs = idleTtlMs > 0 ? idleTtlMs : 5_000L;
        synchronized (lock) {
            discardIdleLocked();
            idle = handle;
            idleDeadlineMs = System.currentTimeMillis() + parkTtlMs;
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
