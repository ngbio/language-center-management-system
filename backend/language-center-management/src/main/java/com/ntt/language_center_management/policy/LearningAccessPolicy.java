package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.PublicationStatus;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.repository.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class LearningAccessPolicy {
  private final EnrollmentRepository enrollments;
  public LearningAccessPolicy(EnrollmentRepository enrollments) {
    this.enrollments = enrollments;
  }

  public boolean accessible(Course course, Student student) {
    return course.getStatus() == CatalogStatus.ACTIVE
        && course.getPublicationStatus() == PublicationStatus.PUBLISHED
        && ((course.getTuitionFee() != null && course.getTuitionFee().signum() == 0)
            || (student != null && enrollments.existsPaidConfirmedAccess(student.getId(), course.getId())));
  }
  public void access(Course course, Student student) {
    if (!accessible(course, student)) throw new ForbiddenException("Khóa học chưa được mở hoặc bạn chưa có quyền học");
  }
  public void access(CourseContent content, Student student) {
    if (content.getPublicationStatus() != PublicationStatus.PUBLISHED) throw missing();
    access(content.getSectionId().getCourseId(), student);
  }
  public void access(Quiz quiz, Student student) {
    if (!"PUBLISHED".equals(quiz.getStatus())) throw missing();
    access(quiz.getContent(), student);
  }
  private ResourceNotFoundException missing() { return new ResourceNotFoundException("Không tìm thấy nội dung học tập"); }

}
