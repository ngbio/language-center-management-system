package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.dto.request.AttendanceBulkRequest;
import com.ntt.language_center_management.dto.request.AttendanceItemRequest;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class AttendanceUpdatePolicy {
  private final int editWindowDays;
  private final java.time.Clock clock;
  public AttendanceUpdatePolicy(@Value("${attendance.edit-window-days:7}") int editWindowDays, java.time.Clock clock) {
    this.editWindowDays = editWindowDays;
    this.clock = clock;
  }

  public void ensureAttendanceAllowed(Lesson lesson) {
    if (lesson.getStatus() == LessonStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể điểm danh buổi học đã hủy");
    }
    if (lesson.getClassScheduleId().getCourseClassId().getStatus() == ClassStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể điểm danh cho lớp học đã hủy");
    }
  }

  public void ensureCanUpdateAttendance(Lesson lesson) {
    ensureAttendanceAllowed(lesson);
    LocalDate lessonDate = ApplicationDateTimeUtils.toLocalDate(lesson.getLessonDate(), clock.getZone());
    LocalDateTime now = LocalDateTime.now(clock);
    LocalDateTime lessonStart =
        LocalDateTime.of(lessonDate,
            ApplicationDateTimeUtils.toLocalTime(
                lesson.getClassScheduleId().getStartTime(), clock.getZone()));
    if (now.isBefore(lessonStart)) {
      throw new IllegalArgumentException("Chỉ được điểm danh sau thời gian bắt đầu buổi học");
    }
    if (now.toLocalDate().isAfter(lessonDate.plusDays(editWindowDays))) {
      throw new IllegalArgumentException(
          "Đã quá thời hạn sửa điểm danh " + editWindowDays + " ngày sau buổi học");
    }
  }
  public Set<Integer> validateStudentIds(AttendanceBulkRequest request) {
    Set<Integer> studentIds = new HashSet<>();
    for (AttendanceItemRequest item : request.attendances()) {
      if (!studentIds.add(item.studentId())) {
        throw new IllegalArgumentException("Danh sách điểm danh chứa học viên bị trùng");
      }
    }

    return studentIds;
  }

  public void validateEnrollmentMembership(Map<Integer, Enrollment> validEnrollments, Set<Integer> studentIds) {
    if (!validEnrollments.keySet().containsAll(studentIds)) {
      throw new IllegalArgumentException("Có học viên không thuộc lớp hoặc chưa thanh toán hợp lệ");
    }

  }

}
