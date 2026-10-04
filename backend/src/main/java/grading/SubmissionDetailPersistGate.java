package com.eiu.capstone.backend.grading;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

@Component
public class SubmissionDetailPersistGate {

    private static final long WAIT_SECONDS = 60;

    private final ConcurrentHashMap<UUID, CompletableFuture<Void>> inflight = new ConcurrentHashMap<>();

    public void runAsync(UUID submissionId, Runnable task, ExecutorService executor) {
        if (submissionId == null) {
            return;
        }
        CompletableFuture<Void> future = new CompletableFuture<>();
        inflight.put(submissionId, future);
        executor.execute(() -> {
            try {
                task.run();
                future.complete(null);
            } catch (Throwable t) {
                System.out.printf("detail persist failed submission=%s: %s%n", submissionId, t);
                if (t.getCause() != null) {
                    System.out.printf("detail persist cause submission=%s: %s%n", submissionId, t.getCause());
                }
                future.completeExceptionally(t);
            } finally {
                inflight.remove(submissionId, future);
            }
        });
    }

    public void await(UUID submissionId) {
        if (submissionId == null) {
            return;
        }
        CompletableFuture<Void> future = inflight.get(submissionId);
        if (future == null) {
            return;
        }
        try {
            future.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.printf("detail persist wait failed submission=%s: %s%n", submissionId, e);
        }
    }
}
