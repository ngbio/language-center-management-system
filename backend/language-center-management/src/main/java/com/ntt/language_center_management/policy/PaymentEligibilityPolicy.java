package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.exception.UnauthorizedException;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class PaymentEligibilityPolicy {

  public void validate(Enrollment enrollment, Student student) {
    if (!enrollment.getStudentId().getId().equals(student.getId())) {
      throw new UnauthorizedException("Bạn không được thanh toán đăng ký của học viên khác");
    }
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.CANCELLED) {
      throw new IllegalArgumentException("Đăng ký đã bị hủy");
    }
    if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED) {
      throw new IllegalArgumentException("Đăng ký không ở trạng thái được phép thanh toán");
    }
    if (enrollment.getPaymentStatus() == EnrollmentPaymentStatus.PAID) {
      throw new IllegalArgumentException("Đăng ký đã được thanh toán");
    }
    if (enrollment.getAmountDue() == null || enrollment.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Đăng ký miễn phí không cần tạo giao dịch thanh toán");
    }

  }

}
