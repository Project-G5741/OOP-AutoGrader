package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GradingExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService gradingExecutor(@Value("${app.grading.parallelism:4}") int parallelism) {
        int poolSize = Math.max(1, Math.min(parallelism, Runtime.getRuntime().availableProcessors()));
        return Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "grading-worker");
            t.setDaemon(true);
            return t;
        });
    }
}
