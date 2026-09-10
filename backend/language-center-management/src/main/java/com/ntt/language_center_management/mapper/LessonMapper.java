package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.response.LessonResponse;
import com.ntt.language_center_management.entity.Lesson;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Component
public class LessonMapper {
  private final ZoneId applicationZone;

  public LessonMapper(@Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone) {
    this.applicationZone = ZoneId.of(applicationTimeZone);
  }

  public LessonResponse toResponse(Lesson lesson) {
    var schedule = lesson.getClassScheduleId();
    var courseClass = schedule.getCourseClassId();
    var room = schedule.getRoomId();
    return new LessonResponse(
        lesson.getId(),
        courseClass.getId(),
        courseClass.getClassCode(),
        courseClass.getClassName(),
        schedule.getId(),
        toLocalDate(lesson.getLessonDate()),
        toLocalTime(schedule.getStartTime()),
        toLocalTime(schedule.getEndTime()),
        lesson.getTopic(),
        lesson.getStatus(),
        schedule.getDeliveryMode(),
        room == null ? null : room.getId(),
        room == null ? null : room.getRoomCode(),
        room == null ? null : room.getRoomName(),
        schedule.getMeetingUrl(),
        toLocalDate(lesson.getOriginalLessonDate()),
        lesson.getRescheduleReason(),
        toLocalDateTime(lesson.getRescheduledAt()));
  }

  private LocalDate toLocalDate(java.util.Date value) {
    if (value == null) {
      return null;
    }
    if (value instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    return Instant.ofEpochMilli(value.getTime()).atZone(applicationZone).toLocalDate();
  }

  private LocalTime toLocalTime(java.util.Date value) {
    if (value instanceof java.sql.Time sqlTime) {
      return sqlTime.toLocalTime();
    }
    return Instant.ofEpochMilli(value.getTime()).atZone(applicationZone).toLocalTime();
  }

  private LocalDateTime toLocalDateTime(java.util.Date value) {
    return value == null ? null : Instant.ofEpochMilli(value.getTime()).atZone(applicationZone).toLocalDateTime();
  }
}
