package com.ntt.language_center_management.unit.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.service.impl.LessonReminderDispatchService;
import com.ntt.language_center_management.service.impl.LessonReminderScheduler;
import org.junit.jupiter.api.Test;

class LessonReminderSchedulerTest {
  @Test
  void continuesFullBatchesAndStopsAfterPartialBatch() {
    LessonReminderDispatchService dispatcher = mock(LessonReminderDispatchService.class);
    when(dispatcher.dispatchBatch(any(), any())).thenReturn(1000, 1000, 12);
    LessonReminderScheduler scheduler =
        new LessonReminderScheduler(dispatcher, "Asia/Ho_Chi_Minh", 10, 20);

    scheduler.sendDueLessonReminders();

    verify(dispatcher, times(3)).dispatchBatch(any(), any());
  }

  @Test
  void respectsMaximumBatchCountUnderHeavyLoad() {
    LessonReminderDispatchService dispatcher = mock(LessonReminderDispatchService.class);
    when(dispatcher.dispatchBatch(any(), any())).thenReturn(1000);
    LessonReminderScheduler scheduler =
        new LessonReminderScheduler(dispatcher, "Asia/Ho_Chi_Minh", 10, 2);

    scheduler.sendDueLessonReminders();

    verify(dispatcher, times(2)).dispatchBatch(any(), any());
  }
}
