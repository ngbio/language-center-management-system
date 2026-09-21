package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import java.time.Clock;
import java.util.Date;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentCancellationPolicy {
  private final Clock clock;

  public EnrollmentCancellationPolicy(Clock clock) { this.clock = clock; }

  public void validate(Enrollment enrollment) {
    Courseclass courseClass = enrollment.getCourseClassId();
    if (!Set.of(ClassStatus.OPEN, ClassStatus.FULL).contains(courseClass.getStatus())) {
      throw new IllegalArgumentException("Không thể hủy đăng ký khi lớp đã bắt đầu hoặc kết thúc");
    }
    if (!courseClass.getStartDate().after(Date.from(clock.instant()))) {
      throw new IllegalArgumentException("Đã quá thời hạn hủy đăng ký trước ngày khai giảng");
    }
    if (enrollment.getPaymentStatus() != EnrollmentPaymentStatus.PENDING) {
      throw new IllegalArgumentException(
          "Đăng ký đã phát sinh thanh toán, cần xử lý hoàn tiền trước khi hủy");
    }
  }
}
