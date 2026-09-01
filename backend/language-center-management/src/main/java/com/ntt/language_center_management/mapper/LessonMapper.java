package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.response.LessonResponse;
import com.ntt.language_center_management.entity.Lesson;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class LessonMapper {

  public LessonResponse toResponse(Lesson lesson) {
    var schedule = lesson.getClassScheduleId();
    var courseClass = schedule.getCourseClassId();
    var room = schedule.getRoomId();
    String meetingUrl =
        lesson.getMeetingUrl() == null ? schedule.getMeetingUrl() : lesson.getMeetingUrl();
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
        meetingUrl);
  }

  private LocalDate toLocalDate(java.util.Date value) {
    return Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private LocalTime toLocalTime(java.util.Date value) {
    return Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalTime();
  }
}
