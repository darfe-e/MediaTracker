package org.example.animetracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

  @Bean(name = "importExecutor")
  public Executor importExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(5);
    executor.setThreadNamePrefix("import-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(120);
    executor.initialize();
    return executor;
  }

  @Bean(name = "catalogCrawlerExecutor")
  public Executor catalogCrawlerExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(1);
    executor.setThreadNamePrefix("catalog-crawler-");
    executor.setWaitForTasksToCompleteOnShutdown(false);
    executor.initialize();
    return executor;
  }
}
