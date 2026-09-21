package com.ntt.language_center_management.factory;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import java.time.Clock;
import java.util.Date;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentFactory {
  private final Clock clock;

  public EnrollmentFactory(Clock clock) { this.clock = clock; }

  public Enrollment createConfirmed(Student student, Courseclass courseClass) {
    Enrollment enrollment = new Enrollment();
    enrollment.setStudentId(student);
    enrollment.setCourseClassId(courseClass);
    Date now = Date.from(clock.instant());
    enrollment.setEnrollmentDate(now);
    enrollment.setPaymentDeadline(Date.from(now.toInstant().plusSeconds(2 * 24 * 60 * 60L)));
    enrollment.setAmountDue(courseClass.getAppliedTuitionFee());
    enrollment.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    enrollment.setPaymentStatus(
        courseClass.getAppliedTuitionFee().compareTo(java.math.BigDecimal.ZERO) == 0
            ? EnrollmentPaymentStatus.PAID : EnrollmentPaymentStatus.PENDING);
    enrollment.setConfirmedAt(now);
    return enrollment;
  }
}
