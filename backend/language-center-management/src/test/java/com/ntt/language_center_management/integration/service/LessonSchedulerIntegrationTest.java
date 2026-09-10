package com.ntt.language_center_management.integration.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.service.impl.LessonCompletionScheduler;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class LessonSchedulerIntegrationTest {
  @Test
  void schedulerCompletesEndedLessonsInVietnamTimeAndSkipsCancelledClasses() {
    LessonRepository repository = Mockito.mock(LessonRepository.class);
    Lesson ended = lesson(ClassStatus.IN_PROGRESS, Time.valueOf("00:01:00"));
    Lesson cancelledClassLesson = lesson(ClassStatus.CANCELLED, Time.valueOf("00:01:00"));
    when(repository.findByStatusInAndLessonDateLessThanEqual(any(), any()))
        .thenReturn(List.of(ended, cancelledClassLesson));

    new LessonCompletionScheduler(repository, "Asia/Ho_Chi_Minh").completeEndedLessons();

    assertEquals(LessonStatus.COMPLETED, ended.getStatus());
    assertEquals(LessonStatus.SCHEDULED, cancelledClassLesson.getStatus());
    verify(repository).saveAll(List.of(ended));
  }

  @Test
  void schedulerDoesNotWriteWhenNoLessonHasEnded() {
    LessonRepository repository = Mockito.mock(LessonRepository.class);
    when(repository.findByStatusInAndLessonDateLessThanEqual(any(), any()))
        .thenReturn(List.of());
    new LessonCompletionScheduler(repository, "Asia/Ho_Chi_Minh").completeEndedLessons();
    verify(repository, never()).saveAll(any());
  }

  private Lesson lesson(ClassStatus classStatus, Time endTime) {
    Courseclass courseClass = new Courseclass();
    courseClass.setStatus(classStatus);
    Classschedule schedule = new Classschedule();
    schedule.setCourseClassId(courseClass);
    schedule.setEndTime(endTime);
    Lesson lesson = new Lesson();
    lesson.setLessonDate(Date.valueOf(LocalDate.now()));
    lesson.setClassScheduleId(schedule);
    lesson.setStatus(LessonStatus.SCHEDULED);
    return lesson;
  }
}
