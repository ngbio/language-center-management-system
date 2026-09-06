package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.Classschedule;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

@Component
public class ClassScheduleMapper {

  public ClassScheduleResponse toResponse(Classschedule schedule) {
    var courseClass = schedule.getCourseClassId();
    var room = schedule.getRoomId();
    return new ClassScheduleResponse(
        schedule.getId(),
        courseClass.getId(),
        courseClass.getClassCode(),
        courseClass.getClassName(),
        schedule.getDayOfWeek(),
        toLocalTime(schedule.getStartTime()),
        toLocalTime(schedule.getEndTime()),
        schedule.getDeliveryMode(),
        room == null ? null : room.getId(),
        room == null ? null : room.getRoomCode(),
        room == null ? null : room.getRoomName(),
        room == null ? null : room.getLocation(),
        schedule.getMeetingUrl());
  }

  private LocalTime toLocalTime(java.util.Date value) {
    return Instant.ofEpochMilli(value.getTime()).atZone(ZoneId.systemDefault()).toLocalTime();
  }
}
