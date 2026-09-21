package com.ntt.language_center_management.service;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import java.time.Clock;
import java.util.Date;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Mutates enrollment state only. Callers own authorization, locking and persistence. */
@Component
public class EnrollmentLifecycle {
  private final Clock clock;

  public EnrollmentLifecycle(Clock clock) { this.clock = clock; }

  public void validateTransition(Enrollment enrollment, EnrollmentStatus requested) {
    if (enrollment.getEnrollmentStatus() == requested) return;
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể thay đổi đăng ký đã hủy");
    }
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.CONFIRMED && requested == EnrollmentStatus.PENDING) {
      throw new IllegalArgumentException("Không thể chuyển đăng ký đã xác nhận về chờ xử lý");
    }
  }

  public void confirm(Enrollment enrollment, Courseclass courseClass) {
    validateTransition(enrollment, EnrollmentStatus.CONFIRMED);
    if (!Set.of(ClassStatus.OPEN, ClassStatus.FULL).contains(courseClass.getStatus())) {
      throw new IllegalArgumentException("Lớp học không còn nhận xử lý đăng ký");
    }
    enrollment.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    enrollment.setConfirmedAt(Date.from(clock.instant()));
  }

  public void cancel(Enrollment enrollment, String reason) {
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.CANCELLED) {
      throw new IllegalArgumentException("Đăng ký đã được hủy trước đó");
    }
    enrollment.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
    enrollment.setCancelledAt(Date.from(clock.instant()));
    enrollment.setCancellationReason(reason.trim());
    if (enrollment.getPaymentStatus() == EnrollmentPaymentStatus.PENDING) {
      enrollment.setPaymentStatus(EnrollmentPaymentStatus.CANCELLED);
    }
  }

  public boolean expire(Enrollment enrollment, Date now) {
    if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED
        || enrollment.getPaymentStatus() != EnrollmentPaymentStatus.PENDING
        || enrollment.getPaymentDeadline() == null
        || !enrollment.getPaymentDeadline().before(now)) return false;
    enrollment.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.CANCELLED);
    enrollment.setCancelledAt(now);
    enrollment.setCancellationReason("Tự động hủy do quá hạn thanh toán 48 giờ");
    return true;
  }

  public void markPaid(Enrollment enrollment, Date now) {
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.CANCELLED) {
      throw new IllegalArgumentException("Đăng ký đã hủy");
    }
    if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED) {
      throw new IllegalArgumentException("Đăng ký không còn hiệu lực");
    }
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PAID);
    if (enrollment.getConfirmedAt() == null) enrollment.setConfirmedAt(now);
  }

  public void markFullyRefunded(Enrollment enrollment, String reason, Date now) {
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.REFUNDED);
    enrollment.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
    enrollment.setCancelledAt(now);
    enrollment.setCancellationReason(reason);
  }
}
