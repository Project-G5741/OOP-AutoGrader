package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class FixedExecutorFactory {

    private FixedExecutorFactory() {}

    static ExecutorService newPool(int parallelism, String threadName) {
        int poolSize = Math.max(1, Math.min(parallelism, Runtime.getRuntime().availableProcessors()));
        return Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, threadName);
            t.setDaemon(true);
            return t;
        });
    }
}
