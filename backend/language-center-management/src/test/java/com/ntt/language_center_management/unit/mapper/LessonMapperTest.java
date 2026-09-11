package com.ntt.language_center_management.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.enums.DeliveryMode;
import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.mapper.LessonMapper;
import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class LessonMapperTest {

  @Test
  void shouldMapSqlDateAndSqlTimeWithoutTimezoneShift() {
    Lesson lesson = new Lesson();
    lesson.setId(1);
    lesson.setLessonDate(Date.valueOf(LocalDate.of(2026, 9, 10)));
    lesson.setStatus(LessonStatus.SCHEDULED);

    Courseclass courseClass = new Courseclass();
    courseClass.setId(11);
    courseClass.setClassCode("CLS001");
    courseClass.setClassName("Beginner Class");

    Classschedule schedule = new Classschedule();
    schedule.setId(22);
    schedule.setCourseClassId(courseClass);
    schedule.setStartTime(Time.valueOf("08:00:00"));
    schedule.setEndTime(Time.valueOf("10:00:00"));
    schedule.setDeliveryMode(DeliveryMode.ONLINE);
    schedule.setMeetingUrl("https://meet.example.com/lesson");
    lesson.setClassScheduleId(schedule);

    var response = new LessonMapper("Asia/Ho_Chi_Minh").toResponse(lesson);

    assertEquals(LocalDate.of(2026, 9, 10), response.lessonDate());
    assertEquals(LocalTime.of(8, 0), response.startTime());
    assertEquals(LocalTime.of(10, 0), response.endTime());
  }
}
