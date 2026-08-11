package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestcaseInvokeExecutorConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService testcaseInvokeExecutor() {
        return FixedExecutorFactory.newPool(1, "testcase-invoke");
    }
}
