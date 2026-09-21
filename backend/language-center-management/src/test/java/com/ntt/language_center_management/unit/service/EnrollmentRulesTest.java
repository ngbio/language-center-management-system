package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.factory.EnrollmentFactory;
import com.ntt.language_center_management.policy.EnrollmentCancellationPolicy;
import com.ntt.language_center_management.policy.EnrollmentEligibilityPolicy;
import com.ntt.language_center_management.policy.EnrollmentTransferPolicy;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.service.EnrollmentLifecycle;
import com.ntt.language_center_management.validation.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class EnrollmentRulesTest {
  private final Instant now = Instant.parse("2026-09-21T08:00:00Z");
  private final Clock clock = Clock.fixed(now, ZoneId.of("Asia/Ho_Chi_Minh"));

  @Test
  void factoryUsesOneInstantAndKeepsFreeAndPaidCourseRules() {
    var factory = new EnrollmentFactory(clock);
    var courseClass = new Courseclass(1);
    var student = new Student(2);
    for (BigDecimal fee : List.of(BigDecimal.ZERO, new BigDecimal("3200000"))) {
      courseClass.setAppliedTuitionFee(fee);
      var enrollment = factory.createConfirmed(student, courseClass);
      assertThat(enrollment.getStudentId()).isSameAs(student);
      assertThat(enrollment.getCourseClassId()).isSameAs(courseClass);
      assertThat(enrollment.getAmountDue()).isEqualTo(fee);
      assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
      assertThat(enrollment.getPaymentStatus()).isEqualTo(fee.signum() == 0
          ? EnrollmentPaymentStatus.PAID : EnrollmentPaymentStatus.PENDING);
      assertThat(enrollment.getEnrollmentDate().toInstant()).isEqualTo(now);
      assertThat(enrollment.getConfirmedAt().toInstant()).isEqualTo(now);
      assertThat(enrollment.getPaymentDeadline().toInstant()).isEqualTo(now.plusSeconds(48 * 3600));
    }
  }

  @Test
  void cancellationRejectsExactStartTimeButAcceptsOneMillisecondBefore() {
    var enrollment = pendingPayment();
    var courseClass = new Courseclass(1);
    courseClass.setStatus(ClassStatus.OPEN);
    enrollment.setCourseClassId(courseClass);
    courseClass.setStartDate(Date.from(now));
    var policy = new EnrollmentCancellationPolicy(clock);
    assertThatThrownBy(() -> policy.validate(enrollment)).hasMessageContaining("quá thời hạn");
    courseClass.setStartDate(Date.from(now.plusMillis(1)));
    policy.validate(enrollment);
  }

  @Test
  void expirationRequiresStrictlyPastDeadlineAndIsIdempotent() {
    var enrollment = pendingPayment();
    var lifecycle = new EnrollmentLifecycle(clock);
    enrollment.setPaymentDeadline(Date.from(now));
    assertThat(lifecycle.expire(enrollment, Date.from(now))).isFalse();
    assertThat(lifecycle.expire(enrollment, Date.from(now.plusMillis(1)))).isTrue();
    assertThat(lifecycle.expire(enrollment, Date.from(now.plusMillis(2)))).isFalse();
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.CANCELLED);
    assertThat(enrollment.getCancelledAt().toInstant()).isEqualTo(now.plusMillis(1));
  }

  @Test
  void rejectedPaymentDoesNotMutateCancelledEnrollment() {
    var enrollment = pendingPayment();
    enrollment.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
    assertThatThrownBy(() -> new EnrollmentLifecycle(clock).markPaid(enrollment, Date.from(now)))
        .hasMessage("Đăng ký đã hủy");
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PENDING);
    assertThat(enrollment.getConfirmedAt()).isNull();
  }

  @Test
  void pipelineRunsInOrderAndStopsBeforeLaterRules() {
    var visited = new ArrayList<Integer>();
    var pipeline = new EnrollmentValidationPipeline(List.of(
        context -> visited.add(1),
        context -> { visited.add(2); throw new IllegalArgumentException("full"); },
        context -> visited.add(3)));
    assertThatThrownBy(() -> pipeline.validate(EnrollmentValidationContext.registration(new Student(1), new Courseclass(2))))
        .hasMessage("full");
    assertThat(visited).containsExactly(1, 2);
  }

  @Test
  void closedClassStopsBeforeDuplicateAndScheduleQueries() {
    var repository = mock(EnrollmentRepository.class);
    var pipelines = new EnrollmentValidationPipelines(new EnrollmentEligibilityPolicy(repository),
        new EnrollmentTransferPolicy(clock));
    var courseClass = new Courseclass(1);
    courseClass.setStatus(ClassStatus.DRAFT);
    assertThatThrownBy(() -> pipelines.validateRegistrationTarget(
        EnrollmentValidationContext.registration(new Student(2), courseClass)))
        .hasMessageContaining("không mở đăng ký");
    verifyNoInteractions(repository);
  }

  @Test
  void transferRejectsDifferentCourseBeforeCheckingCapacity() {
    var repository = mock(EnrollmentRepository.class);
    var pipelines = new EnrollmentValidationPipelines(new EnrollmentEligibilityPolicy(repository),
        new EnrollmentTransferPolicy(clock));
    var source = new Courseclass(1);
    source.setCourseId(new Course(3));
    var target = new Courseclass(2);
    target.setCourseId(new Course(4));
    assertThatThrownBy(() -> pipelines.validateTransferTarget(
        EnrollmentValidationContext.transfer(new Student(5), source, target, 10)))
        .hasMessageContaining("cùng khóa học");
    verifyNoInteractions(repository);
  }

  private Enrollment pendingPayment() {
    var enrollment = new Enrollment(1);
    enrollment.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PENDING);
    return enrollment;
  }
}
