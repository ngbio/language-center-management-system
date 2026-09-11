package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.service.EnrollmentExpirationService;
import java.util.Date;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.ntt.language_center_management.policy.EnrollmentPolicy.CAPACITY_RESERVED_STATUSES;

@Service
public class EnrollmentExpirationServiceImpl implements EnrollmentExpirationService {
  private final EnrollmentRepository enrollmentRepository;
  private final CourseClassRepository courseClassRepository;

  public EnrollmentExpirationServiceImpl(EnrollmentRepository enrollmentRepository,
      CourseClassRepository courseClassRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.courseClassRepository = courseClassRepository;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean expireIfOverdue(Integer enrollmentId) {
    Enrollment enrollment = enrollmentRepository.lockById(enrollmentId).orElse(null);
    return enrollment != null && expire(enrollment, new Date());
  }

  @Override
  @Scheduled(fixedDelayString = "${app.enrollment.expiration-check-ms:60000}")
  @Transactional
  public void expireOverdueEnrollments() {
    Date now = new Date();
    for (Integer id : enrollmentRepository.findExpiredPendingIds(now)) {
      enrollmentRepository.lockById(id).ifPresent(enrollment -> expire(enrollment, now));
    }
  }

  private boolean expire(Enrollment enrollment, Date now) {
    if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED
        || enrollment.getPaymentStatus() != EnrollmentPaymentStatus.PENDING
        || enrollment.getPaymentDeadline() == null
        || !enrollment.getPaymentDeadline().before(now)) return false;
    enrollment.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.CANCELLED);
    enrollment.setCancelledAt(now);
    enrollment.setCancellationReason("Tự động hủy do quá hạn thanh toán 48 giờ");
    enrollmentRepository.saveAndFlush(enrollment);
    Courseclass courseClass = enrollment.getCourseClassId();
    long occupied = enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(
        courseClass.getId(), CAPACITY_RESERVED_STATUSES);
    if (courseClass.getStatus() == ClassStatus.FULL && occupied < courseClass.getMaxStudents()) {
      courseClass.setStatus(ClassStatus.OPEN);
      courseClassRepository.save(courseClass);
    }
    return true;
  }
}
