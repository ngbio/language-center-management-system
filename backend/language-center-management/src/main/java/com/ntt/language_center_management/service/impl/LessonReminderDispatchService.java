package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.repository.NotificationRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonReminderDispatchService {
  static final int BATCH_SIZE = 1000;
  private final NotificationRepository notifications;

  public LessonReminderDispatchService(NotificationRepository notifications) {
    this.notifications = notifications;
  }

  @Transactional
  public int dispatchBatch(LocalDateTime windowStart, LocalDateTime now) {
    return notifications.insertDueLessonReminderBatch(windowStart, now, now);
  }
}
