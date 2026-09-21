package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.policy.EnrollmentPolicy;
import com.ntt.language_center_management.dto.request.RefundRequest;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Refund;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.RefundStatus;
import com.ntt.language_center_management.repository.RefundRepository;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class RefundEligibilityPolicy {
  private final RefundRepository refundRepository;
  public RefundEligibilityPolicy(RefundRepository refundRepository) {
    this.refundRepository = refundRepository;
  }

  public void validateIdempotency(Refund previous, Integer enrollmentId, RefundRequest request) {
      if (!previous.getEnrollment().getId().equals(enrollmentId)
          || (request.amount() != null && previous.getAmount().compareTo(request.amount()) != 0)) {
        throw new IllegalArgumentException("Idempotency key đã được dùng cho yêu cầu hoàn tiền khác");
      }
  }
  public void validateEligibility(Enrollment enrollment, List<Payment> paidPayments) {
    if (paidPayments.isEmpty() || enrollment.getPaymentStatus() != EnrollmentPaymentStatus.PAID) {
      throw new IllegalArgumentException("Đăng ký chưa có khoản thanh toán thành công để hoàn");
    }
    if (refundRepository.existsByEnrollment_IdAndStatus(enrollment.getId(), RefundStatus.PENDING)) {
      throw new IllegalArgumentException("Đăng ký đang có một yêu cầu hoàn tiền chờ xử lý");
    }
  }
  public void validateAmount(BigDecimal amount, BigDecimal refundable) {
    if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(refundable) > 0) {
      throw new IllegalArgumentException("Số tiền hoàn vượt quá số tiền thực thu còn lại: " + refundable);
    }

  }

}
