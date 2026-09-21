package com.ntt.language_center_management.validation;

import com.ntt.language_center_management.policy.EnrollmentEligibilityPolicy;
import com.ntt.language_center_management.policy.EnrollmentTransferPolicy;
import java.util.List;
import org.springframework.stereotype.Component;

/** Explicit use-case compositions; no implicit collection of all rule beans. */
@Component
public class EnrollmentValidationPipelines {
  private final EnrollmentValidationPipeline registrationTarget;
  private final EnrollmentValidationPipeline transferTarget;

  public EnrollmentValidationPipelines(EnrollmentEligibilityPolicy eligibility, EnrollmentTransferPolicy transfer) {
    EnrollmentRule capacity = context -> eligibility.validateOpenClassAndCapacity(context.targetClass());
    EnrollmentRule schedule = context -> eligibility.validateNoScheduleConflict(
        context.student().getId(), context.targetClass().getId());
    registrationTarget = new EnrollmentValidationPipeline(List.of(
        capacity,
        context -> eligibility.validateNotAlreadyEnrolled(context.student(), context.targetClass(),
            "Học viên đang đăng ký lớp học này"),
        schedule));
    transferTarget = new EnrollmentValidationPipeline(List.of(
        context -> transfer.validateSameCourse(context.sourceClass(), context.targetClass()),
        capacity,
        context -> eligibility.validateNotAlreadyEnrolled(context.student(), context.targetClass(),
            "Học viên đang đăng ký lớp chuyển đến"),
        context -> eligibility.validateTransferSchedule(context.student().getId(),
            context.targetClass().getId(), context.sourceEnrollmentId())));
  }

  public void validateRegistrationTarget(EnrollmentValidationContext context) {
    registrationTarget.validate(context);
  }

  public void validateTransferTarget(EnrollmentValidationContext context) {
    transferTarget.validate(context);
  }
}
