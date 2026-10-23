package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CompileExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService compileExecutor(@Value("${app.compile.parallelism:4}") int parallelism) {
        return FixedExecutorFactory.newPool(parallelism, "compile-worker");
    }
}
