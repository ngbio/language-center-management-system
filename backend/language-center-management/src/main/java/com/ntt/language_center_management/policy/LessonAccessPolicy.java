package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import java.security.Principal;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class LessonAccessPolicy {
  private final StudentRepository studentRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CurrentUserResolver currentUserResolver;
  public LessonAccessPolicy(StudentRepository studentRepository, EnrollmentRepository enrollmentRepository, CurrentUserResolver currentUserResolver) {
    this.studentRepository = studentRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.currentUserResolver = currentUserResolver;
  }

  public void ensureClassMember(Courseclass courseClass, Principal principal) {
    User user = findUser(principal);
    String role = user.getRoleId().getRoleCode();
    if (Set.of("ADMIN", "CONSULTANT").contains(role)) {
      return;
    }
    if ("TEACHER".equals(role)
        && courseClass.getTeacherId() != null
        && courseClass.getTeacherId().getUserId().getId().equals(user.getId())) {
      return;
    }
    if ("STUDENT".equals(role)) {
      Student student =
          studentRepository
              .findByUserId_EmailIgnoreCase(user.getEmail())
              .orElseThrow(() -> new ForbiddenException("Không có hồ sơ học viên hợp lệ"));
      if (enrollmentRepository
          .existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusAndPaymentStatus(
              student.getId(), courseClass.getId(), EnrollmentStatus.CONFIRMED,
              EnrollmentPaymentStatus.PAID)) {
        return;
      }
    }
    throw new ForbiddenException("Bạn không phải thành viên của lớp học này");
  }

  public User ensureCanEditContent(Courseclass courseClass, Principal principal) {
    User user = findUser(principal);
    String role = user.getRoleId().getRoleCode();
    if (Set.of("ADMIN", "CONSULTANT").contains(role)) {
      return user;
    }
    if ("TEACHER".equals(role)
        && courseClass.getTeacherId() != null
        && courseClass.getTeacherId().getUserId().getId().equals(user.getId())) {
      return user;
    }
    throw new ForbiddenException("Bạn không được sửa nội dung buổi học này");
  }

  private User findUser(Principal principal) {
    return currentUserResolver.requireUser(principal);
  }
}
