package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import org.springframework.stereotype.Component;
import static com.ntt.language_center_management.policy.EnrollmentPolicy.CAPACITY_RESERVED_STATUSES;

@Component
public class EnrollmentEligibilityPolicy {
  private final EnrollmentRepository enrollmentRepository;

  public EnrollmentEligibilityPolicy(EnrollmentRepository enrollmentRepository) {
    this.enrollmentRepository = enrollmentRepository;
  }

  public void validateActiveStudent(Student student) {
    if (student.getUserId() == null || student.getUserId().getStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException("Tài khoản học viên không ở trạng thái ACTIVE");
    }
  }

  public void validateOpenClassAndCapacity(Courseclass courseClass) {
    if (courseClass.getStatus() != ClassStatus.OPEN) {
      throw new IllegalArgumentException("Lớp học hiện không mở đăng ký");
    }
    if (enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(
        courseClass.getId(), CAPACITY_RESERVED_STATUSES) >= courseClass.getMaxStudents()) {
      throw new IllegalArgumentException("Lớp học đã đủ số lượng học viên");
    }
  }

  public void validateNoScheduleConflict(Integer studentId, Integer courseClassId) {
    if (enrollmentRepository.existsScheduleConflict(
        studentId, courseClassId, CAPACITY_RESERVED_STATUSES)) {
      throw new IllegalArgumentException("Lớp học bị trùng thời gian với đăng ký hiện tại");
    }
  }

  public void validateTransferSchedule(Integer studentId, Integer targetClassId, Integer sourceEnrollmentId) {
    if (enrollmentRepository.existsScheduleConflictExcludingEnrollment(
        studentId, sourceEnrollmentId, targetClassId, CAPACITY_RESERVED_STATUSES)) {
      throw new IllegalArgumentException("Lớp học bị trùng thời gian với đăng ký hiện tại");
    }
  }

  public void validateNotAlreadyEnrolled(Student student, Courseclass courseClass, String message) {
    if (enrollmentRepository.existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusIn(
        student.getId(), courseClass.getId(), CAPACITY_RESERVED_STATUSES)) {
      throw new DuplicateResourceException(message);
    }
  }
}
