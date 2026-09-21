package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.policy.EnrollmentPolicy;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class CourseClassOpeningPolicy {
  private final ClassScheduleRepository classScheduleRepository;
  private final java.time.Clock clock;
  public CourseClassOpeningPolicy(ClassScheduleRepository classScheduleRepository, java.time.Clock clock) {
    this.classScheduleRepository = classScheduleRepository;
    this.clock = clock;
  }

  private LocalDate toLocalDate(Date value) { return ApplicationDateTimeUtils.toLocalDate(value, clock.getZone()); }

  public void validateCanOpen(Courseclass value) {
    if (value.getCourseId().getStatus() != CatalogStatus.ACTIVE) {
      throw new IllegalArgumentException("Khóa học không hoạt động");
    }
    if (value.getTeacherId() == null) {
      throw new IllegalArgumentException("Phải phân công giảng viên trước khi mở lớp");
    }
    ensureTeacherActive(value.getTeacherId());
    if (toLocalDate(value.getStartDate()).isBefore(LocalDate.now(clock))) {
      throw new IllegalArgumentException("Không thể mở lớp đã qua ngày bắt đầu");
    }
    validateSchedulesAndConflicts(value);
  }

  public void validateSchedulesAndConflicts(Courseclass value) {
    List<Classschedule> schedules = classScheduleRepository.findByCourseClassId_Id(value.getId());
    if (schedules.isEmpty()) {
      throw new IllegalArgumentException("Lớp phải có ít nhất một lịch học hợp lệ");
    }
    for (Classschedule schedule : schedules) {
      if (schedule.getDayOfWeek() < 1
          || schedule.getDayOfWeek() > 7
          || !schedule.getStartTime().before(schedule.getEndTime())) {
        throw new IllegalArgumentException("Lịch học của lớp không hợp lệ");
      }
      Integer roomId = schedule.getRoomId() == null ? null : schedule.getRoomId().getId();
      Integer teacherId = value.getTeacherId() == null ? null : value.getTeacherId().getId();
      if (classScheduleRepository.existsConflict(
          value.getId(),
          roomId,
          teacherId,
          value.getStartDate(),
          value.getEndDate(),
          schedule.getDayOfWeek(),
          schedule.getStartTime(),
          schedule.getEndTime())) {
        throw new IllegalArgumentException("Lịch học bị trùng phòng hoặc giảng viên");
      }
    }
    validateInternalScheduleConflicts(schedules);
  }

  public void validateInternalScheduleConflicts(List<Classschedule> schedules) {
    for (int i = 0; i < schedules.size(); i++) {
      Classschedule first = schedules.get(i);
      for (int j = i + 1; j < schedules.size(); j++) {
        Classschedule second = schedules.get(j);
        boolean sameDay = first.getDayOfWeek() == second.getDayOfWeek();
        boolean overlap =
            first.getStartTime().before(second.getEndTime())
                && first.getEndTime().after(second.getStartTime());
        if (sameDay && overlap) {
          throw new IllegalArgumentException("Các lịch trong cùng lớp bị chồng thời gian");
        }
      }
    }
  }

  public void ensureTeacherActive(Teacher teacher) {
    if (teacher.getUserId().getStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException("Giảng viên không hoạt động");
    }
  }
}
