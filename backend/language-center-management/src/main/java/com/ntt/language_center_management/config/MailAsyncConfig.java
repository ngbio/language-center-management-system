package com.ntt.language_center_management.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class MailAsyncConfig {

  @Bean(name = "mailTaskExecutor")
  public ThreadPoolTaskExecutor mailTaskExecutor(
      @Value("${app.mail.executor.core-pool-size:2}") int corePoolSize,
      @Value("${app.mail.executor.max-pool-size:4}") int maxPoolSize,
      @Value("${app.mail.executor.queue-capacity:100}") int queueCapacity) {
    int normalizedCorePoolSize = Math.max(1, corePoolSize);
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(normalizedCorePoolSize);
    executor.setMaxPoolSize(Math.max(normalizedCorePoolSize, maxPoolSize));
    executor.setQueueCapacity(Math.max(1, queueCapacity));
    executor.setThreadNamePrefix("mail-notification-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return executor;
  }
}
