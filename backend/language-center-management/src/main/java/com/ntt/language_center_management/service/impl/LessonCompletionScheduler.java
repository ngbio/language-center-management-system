package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.enums.ClassStatus;

import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.repository.LessonRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonCompletionScheduler {
  private final LessonRepository lessonRepository;
  private final ZoneId applicationZone;

  public LessonCompletionScheduler(
      LessonRepository lessonRepository,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone) {
    this.lessonRepository = lessonRepository;
    this.applicationZone = ZoneId.of(applicationTimeZone);
  }

  @Scheduled(
      fixedDelayString = "${app.lesson.completion-check-ms:60000}",
      initialDelayString = "${app.lesson.completion-initial-delay-ms:10000}")
  @Transactional
  public void completeEndedLessons() {
    LocalDateTime now = LocalDateTime.now(applicationZone);
    List<Lesson> endedLessons =
        lessonRepository
            .findByStatusInAndLessonDateLessThanEqual(
                List.of(LessonStatus.SCHEDULED, LessonStatus.IN_PROGRESS),
                java.sql.Date.valueOf(now.toLocalDate()))
            .stream()
            .filter(lesson -> lesson.getClassScheduleId().getCourseClassId().getStatus()
                != ClassStatus.CANCELLED)
            .filter(lesson -> !now.isBefore(endTimeOf(lesson)))
            .toList();
    endedLessons.forEach(lesson -> lesson.setStatus(LessonStatus.COMPLETED));
    if (!endedLessons.isEmpty()) {
      lessonRepository.saveAll(endedLessons);
    }
  }

  private LocalDateTime endTimeOf(Lesson lesson) {
    return LocalDateTime.of(
        toLocalDate(lesson.getLessonDate()),
        toLocalTime(lesson.getClassScheduleId().getEndTime()));
  }

  private LocalDate toLocalDate(Date value) {
    if (value instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    return value.toInstant().atZone(applicationZone).toLocalDate();
  }

  private LocalTime toLocalTime(Date value) {
    if (value instanceof java.sql.Time sqlTime) {
      return sqlTime.toLocalTime();
    }
    return value.toInstant().atZone(applicationZone).toLocalTime();
  }
}
