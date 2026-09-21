package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.dto.request.LessonRescheduleRequest;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.repository.AttendanceRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class LessonChangePolicy {
  private final AttendanceRepository attendanceRepository;
  private final LessonRepository lessonRepository;
  public LessonChangePolicy(AttendanceRepository attendanceRepository, LessonRepository lessonRepository) {
    this.attendanceRepository = attendanceRepository;
    this.lessonRepository = lessonRepository;
  }

  public void ensureNoAttendance(Lesson lesson) {
    if (attendanceRepository.existsByLessonId_Id(lesson.getId())) {
      throw new IllegalArgumentException("Không thể dời hoặc hủy buổi học đã có điểm danh");
    }
  }

  public void ensureClassAllowsLessonChanges(Courseclass courseClass) {
    if (courseClass.getStatus() == ClassStatus.COMPLETED
        || courseClass.getStatus() == ClassStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể thay đổi buổi học của lớp đã kết thúc hoặc đã hủy");
    }
  }

  public void validateActualConflict(
      Integer lessonId, Courseclass courseClass, Classschedule schedule, LocalDate date) {
    Integer roomId = schedule.getRoomId() == null ? null : schedule.getRoomId().getId();
    Integer teacherId = courseClass.getTeacherId() == null ? null : courseClass.getTeacherId().getId();
    if (lessonRepository.existsResourceConflictOnDate(
        lessonId,
        courseClass.getId(),
        roomId,
        teacherId,
        toDate(date),
        schedule.getStartTime(),
        schedule.getEndTime())) {
      throw new DuplicateResourceException("Buổi học bị trùng phòng hoặc giảng viên");
    }
  }

  private Date toDate(LocalDate value) {
    return java.sql.Date.valueOf(value);
  }
  public void validateContentUpdate(Lesson lesson) {
    if (Set.of(LessonStatus.COMPLETED, LessonStatus.CANCELLED).contains(lesson.getStatus())) {
      throw new IllegalArgumentException("Không thể sửa nội dung buổi học đã hoàn thành hoặc đã hủy");
    }
  }

  public void validateReschedule(Lesson lesson, LessonRescheduleRequest request, LocalDateTime now, ZoneId applicationZone) {
    Courseclass courseClass = lesson.getClassScheduleId().getCourseClassId();
    if (Set.of(LessonStatus.COMPLETED, LessonStatus.CANCELLED).contains(lesson.getStatus())) {
      throw new IllegalArgumentException("Không thể dời buổi học đã hoàn thành hoặc đã hủy");
    }
    ensureNoAttendance(lesson);
    ensureClassAllowsLessonChanges(courseClass);

    LocalDate currentLessonDate = ApplicationDateTimeUtils.toLocalDate(lesson.getLessonDate(), applicationZone);
    LocalTime lessonStartTime = ApplicationDateTimeUtils.toLocalTime(lesson.getClassScheduleId().getStartTime(), applicationZone);
    if (!now.isBefore(LocalDateTime.of(currentLessonDate, lessonStartTime))) {
      throw new IllegalArgumentException("Chỉ được dời buổi học chưa bắt đầu");
    }

    LocalDate lessonDate = request.lessonDate();
    LocalDate startDate = ApplicationDateTimeUtils.toLocalDate(courseClass.getStartDate(), applicationZone);
    LocalDate endDate = ApplicationDateTimeUtils.toLocalDate(courseClass.getEndDate(), applicationZone);
    if (lessonDate.isBefore(startDate) || lessonDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Ngày học mới phải nằm trong khoảng thời gian của lớp");
    }
    if (lessonDate.equals(currentLessonDate)) {
      throw new IllegalArgumentException("Ngày học mới phải khác ngày học hiện tại");
    }
    if (!LocalDateTime.of(lessonDate, lessonStartTime).isAfter(now)) {
      throw new IllegalArgumentException("Ngày học mới phải là một thời điểm chưa diễn ra");
    }
    Date newDate = toDate(lessonDate);
    if (lessonRepository.existsByClassScheduleId_IdAndLessonDateAndIdNot(
        lesson.getClassScheduleId().getId(), newDate, lesson.getId())) {
      throw new DuplicateResourceException("Lịch này đã có buổi học trong ngày được chọn");
    }
    validateActualConflict(lesson.getId(), courseClass, lesson.getClassScheduleId(), lessonDate);
  }

  public void validateCancellation(Lesson lesson) {
    if (lesson.getStatus() == LessonStatus.COMPLETED) {
      throw new IllegalArgumentException("Không thể hủy buổi học đã hoàn thành");
    }
    ensureNoAttendance(lesson);
    ensureClassAllowsLessonChanges(lesson.getClassScheduleId().getCourseClassId());
    if (lesson.getStatus() == LessonStatus.CANCELLED) {
      throw new IllegalArgumentException("Buổi học đã được hủy trước đó");
    }
  }

  public void validateGenerationStart(Courseclass courseClass, User editor, LocalDate today, ZoneId applicationZone) {
    if ("TEACHER".equals(editor.getRoleId().getRoleCode())
        && today.isBefore(ApplicationDateTimeUtils.toLocalDate(courseClass.getStartDate(), applicationZone))) {
      throw new IllegalArgumentException(
          "Giảng viên chỉ được sinh buổi học từ ngày khai giảng của lớp");
    }
  }

  public void validateSchedulesPresent(List<Classschedule> schedules) {
    if (schedules.isEmpty()) {
      throw new IllegalArgumentException("Lớp chưa có lịch học để sinh buổi");
    }

  }

  public void validateExistingCount(int existingCount, int totalSessions) {
    if (existingCount > totalSessions) {
      throw new IllegalArgumentException("Số buổi hiện có đã vượt tổng số buổi của khóa học");
    }
  }

  public void validateGeneratedCount(int generatedCount, int remaining, int totalSessions) {
    if (generatedCount != remaining) {
      throw new IllegalArgumentException(
          "Khoảng ngày và lịch lặp không đủ để sinh " + totalSessions + " buổi học");
    }
  }

}
