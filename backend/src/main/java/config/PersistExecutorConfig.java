package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService persistExecutor() {
        // I/O-bound: detail UPSERT, rubric overlap, sidecars, temp delete, plagiarism inspect.
        // Do not CPU-cap — 1-CPU Render still needs a free thread while another waits on Neon.
        return Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "persist-worker");
            t.setDaemon(true);
            return t;
        });
    }
}
