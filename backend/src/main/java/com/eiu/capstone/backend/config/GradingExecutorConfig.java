package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GradingExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService gradingExecutor(@Value("${app.grading.parallelism:4}") int parallelism) {
        return FixedExecutorFactory.newPool(parallelism, "grading-worker");
    }
}
