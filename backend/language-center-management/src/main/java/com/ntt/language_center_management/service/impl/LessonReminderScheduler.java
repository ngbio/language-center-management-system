package com.ntt.language_center_management.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LessonReminderScheduler {
  private static final Logger log = LoggerFactory.getLogger(LessonReminderScheduler.class);
  private final LessonReminderDispatchService dispatcher;
  private final ZoneId applicationZone;
  private final int lookbackMinutes;
  private final int maxBatches;

  public LessonReminderScheduler(
      LessonReminderDispatchService dispatcher,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone,
      @Value("${app.notification.lesson-reminder-lookback-minutes:10}") int lookbackMinutes,
      @Value("${app.notification.lesson-reminder-max-batches:20}") int maxBatches) {
    this.dispatcher = dispatcher;
    this.applicationZone = ZoneId.of(applicationTimeZone);
    this.lookbackMinutes = Math.max(1, lookbackMinutes);
    this.maxBatches = Math.max(1, maxBatches);
  }

  @Scheduled(
      fixedDelayString = "${app.notification.lesson-reminder-check-ms:60000}",
      initialDelayString = "${app.notification.lesson-reminder-initial-delay-ms:15000}")
  public void sendDueLessonReminders() {
    LocalDateTime now = LocalDateTime.now(applicationZone);
    LocalDateTime windowStart = now.minusMinutes(lookbackMinutes);
    int total = 0;
    for (int batch = 0; batch < maxBatches; batch++) {
      int inserted = dispatcher.dispatchBatch(windowStart, now);
      total += inserted;
      if (inserted < LessonReminderDispatchService.BATCH_SIZE) break;
    }
    if (total > 0) {
      log.info("Created {} due lesson notifications", total);
    }
  }
}
