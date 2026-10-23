package com.eiu.capstone.backend.config;

import java.util.concurrent.Semaphore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestcaseInvokeExecutorConfig {

    /** Host-wide isolated worker slot. Acquire on the HTTP request thread only. */
    @Bean
    public Semaphore workerJvmSlot() {
        return new Semaphore(1);
    }
}
