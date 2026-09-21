package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import java.time.Clock;
import java.util.Date;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentTransferPolicy {
  private final Clock clock;

  public EnrollmentTransferPolicy(Clock clock) { this.clock = clock; }

  public void validateSource(Enrollment enrollment) {
    if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED
        || enrollment.getPaymentStatus() != EnrollmentPaymentStatus.PENDING) {
      throw new IllegalArgumentException(
          "Chỉ được chuyển lớp cho đăng ký đã xác nhận và chưa thanh toán");
    }
    validateSourceClass(enrollment.getCourseClassId());
  }

  public void validateDifferentClass(Integer sourceId, Integer targetId) {
    if (sourceId.equals(targetId)) throw new IllegalArgumentException("Lớp chuyển đến phải khác lớp hiện tại");
  }

  public void validateSameCourse(Courseclass source, Courseclass target) {
    if (!source.getCourseId().getId().equals(target.getCourseId().getId())) {
      throw new IllegalArgumentException("Chỉ được chuyển sang lớp thuộc cùng khóa học");
    }
  }

  private void validateSourceClass(Courseclass sourceClass) {
    if (!Set.of(ClassStatus.OPEN, ClassStatus.FULL).contains(sourceClass.getStatus())) {
      throw new IllegalArgumentException("Không thể chuyển khi lớp hiện tại đã bắt đầu hoặc kết thúc");
    }
    if (!sourceClass.getStartDate().after(Date.from(clock.instant()))) {
      throw new IllegalArgumentException("Đã quá thời hạn chuyển lớp trước ngày khai giảng");
    }
  }
}
