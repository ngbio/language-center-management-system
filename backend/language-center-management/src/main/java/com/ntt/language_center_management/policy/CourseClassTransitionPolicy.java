package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.policy.EnrollmentPolicy;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.entity.Courseclass;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class CourseClassTransitionPolicy {

  private static final Map<ClassStatus, Set<ClassStatus>> TRANSITIONS =
      Map.of(
          ClassStatus.DRAFT, Set.of(ClassStatus.OPEN, ClassStatus.CANCELLED),
          ClassStatus.OPEN, Set.of(ClassStatus.FULL, ClassStatus.IN_PROGRESS, ClassStatus.CANCELLED),
          ClassStatus.FULL, Set.of(ClassStatus.OPEN, ClassStatus.IN_PROGRESS,
              ClassStatus.COMPLETED, ClassStatus.CANCELLED),
          ClassStatus.IN_PROGRESS, Set.of(ClassStatus.COMPLETED, ClassStatus.CANCELLED),
          ClassStatus.COMPLETED, Set.of(),
          ClassStatus.CANCELLED, Set.of());
  public void validateTransition(Courseclass value, ClassStatus status) {
    if (status == null) {
      throw new IllegalArgumentException("Trạng thái lớp không được để trống");
    }
    ClassStatus normalizedStatus = status;
    if (!TRANSITIONS.getOrDefault(value.getStatus(), Set.of()).contains(normalizedStatus)) {
      throw new IllegalArgumentException(
          "Không thể chuyển trạng thái từ " + value.getStatus() + " sang " + normalizedStatus);
    }
  }
  public void validateFull(Courseclass value, long activeEnrollments) {
    if (activeEnrollments < value.getMaxStudents()) {
      throw new IllegalArgumentException("Chỉ có thể chuyển FULL khi lớp đã đủ sĩ số");
    }
  }

}
