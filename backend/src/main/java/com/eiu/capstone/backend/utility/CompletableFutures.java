package com.eiu.capstone.backend.utility;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

public final class CompletableFutures {

    private CompletableFutures() {}

    public static <T> List<T> joinAll(List<CompletableFuture<T>> futures) {
        try {
            return futures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());
        } catch (CompletionException e) {
            throw unwrap(e);
        }
    }

    public static RuntimeException unwrap(CompletionException e) {
        Throwable cause = e.getCause();
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        if (cause instanceof Error error) {
            throw error;
        }
        return new RuntimeException(cause != null ? cause : e);
    }
}
