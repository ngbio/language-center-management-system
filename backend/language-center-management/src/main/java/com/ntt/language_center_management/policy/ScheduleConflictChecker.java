package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import java.time.LocalDate;
import java.util.Date;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class ScheduleConflictChecker {
  private final ClassScheduleRepository classScheduleRepository;
  private final LessonRepository lessonRepository;
  private final java.time.Clock clock;
  public ScheduleConflictChecker(ClassScheduleRepository classScheduleRepository, LessonRepository lessonRepository, java.time.Clock clock) {
    this.classScheduleRepository = classScheduleRepository;
    this.lessonRepository = lessonRepository;
    this.clock = clock;
  }

  private LocalDate toLocalDate(Date value) { return ApplicationDateTimeUtils.toLocalDate(value, clock.getZone()); }
  private Date toDate(LocalDate value) { return java.sql.Date.valueOf(value); }

  public void validateConflicts(Classschedule schedule) {
    Courseclass courseClass = schedule.getCourseClassId();
    Integer scheduleId = schedule.getId();
    Integer roomId = schedule.getRoomId() == null ? null : schedule.getRoomId().getId();
    Integer teacherId = courseClass.getTeacherId() == null ? null : courseClass.getTeacherId().getId();

    if (classScheduleRepository.existsClassTimeConflict(
        scheduleId,
        courseClass.getId(),
        schedule.getDayOfWeek(),
        schedule.getStartTime(),
        schedule.getEndTime())) {
      throw new DuplicateResourceException("Lịch mới bị chồng thời gian với lịch khác trong lớp");
    }

    if (classScheduleRepository.existsResourceConflict(
        scheduleId,
        roomId,
        teacherId,
        courseClass.getStartDate(),
        courseClass.getEndDate(),
        schedule.getDayOfWeek(),
        schedule.getStartTime(),
        schedule.getEndTime())) {
      throw new DuplicateResourceException("Lịch học bị trùng phòng hoặc giảng viên");
    }

    LocalDate date = toLocalDate(courseClass.getStartDate());
    LocalDate endDate = toLocalDate(courseClass.getEndDate());
    while (!date.isAfter(endDate)) {
      if (date.getDayOfWeek().getValue() == schedule.getDayOfWeek()
          && lessonRepository.existsResourceConflictOnDate(
              null,
              courseClass.getId(),
              roomId,
              teacherId,
              toDate(date),
              schedule.getStartTime(),
              schedule.getEndTime())) {
        throw new DuplicateResourceException("Lịch học bị trùng với buổi học thực tế đã có");
      }
      date = date.plusDays(1);
    }
  }
}
