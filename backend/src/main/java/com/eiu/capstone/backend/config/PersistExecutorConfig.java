package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PersistExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService persistExecutor() {
        return FixedExecutorFactory.newPool(2, "persist-worker");
    }
}
