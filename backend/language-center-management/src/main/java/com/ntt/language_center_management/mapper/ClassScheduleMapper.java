package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import java.time.LocalTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClassScheduleMapper {
  private final ZoneId applicationZone;

  public ClassScheduleMapper(
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone) {
    this.applicationZone = ZoneId.of(applicationTimeZone);
  }

  public ClassScheduleResponse toResponse(Classschedule schedule) {
    return toResponse(schedule, true);
  }

  public ClassScheduleResponse toPublicResponse(Classschedule schedule) {
    return toResponse(schedule, false);
  }

  private ClassScheduleResponse toResponse(Classschedule schedule, boolean includeMeetingUrl) {
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
        includeMeetingUrl ? schedule.getMeetingUrl() : null);
  }

  private LocalTime toLocalTime(java.util.Date value) {
    return ApplicationDateTimeUtils.toLocalTime(value, applicationZone);
  }
}
