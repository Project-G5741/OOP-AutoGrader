package com.eiu.capstone.backend.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PillarExecutorConfig {

  @Bean(destroyMethod = "shutdown")
  public ExecutorService pillarExecutor(@Value("${app.grading.parallelism:4}") int parallelism) {
    // MMD + testcase pillars run inside challenge workers on gradingExecutor.
    // This pool must be separate so challenge workers never block waiting for
    // pillar tasks on the same fixed pool (deadlock on 1–2 CPU hosts e.g. Render).
    int poolSize = Math.max(2, parallelism * 2);
    return Executors.newFixedThreadPool(poolSize, r -> {
      Thread t = new Thread(r, "pillar-worker");
      t.setDaemon(true);
      return t;
    });
  }
}
