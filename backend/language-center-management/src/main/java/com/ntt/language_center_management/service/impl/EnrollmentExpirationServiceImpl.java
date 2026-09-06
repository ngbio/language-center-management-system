package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.service.EnrollmentExpirationService;
import java.util.Date;
import java.util.Set;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentExpirationServiceImpl implements EnrollmentExpirationService {
  private static final Set<String> ACTIVE_STATUSES = Set.of("PENDING", "CONFIRMED");
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
    if (!"CONFIRMED".equals(enrollment.getEnrollmentStatus())
        || !"PENDING".equals(enrollment.getPaymentStatus())
        || enrollment.getPaymentDeadline() == null
        || !enrollment.getPaymentDeadline().before(now)) return false;
    enrollment.setEnrollmentStatus("CANCELLED");
    enrollment.setPaymentStatus("CANCELLED");
    enrollment.setCancelledAt(now);
    enrollment.setCancellationReason("Tự động hủy do quá hạn thanh toán 48 giờ");
    enrollmentRepository.saveAndFlush(enrollment);
    Courseclass courseClass = enrollment.getCourseClassId();
    long occupied = enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(
        courseClass.getId(), ACTIVE_STATUSES);
    if ("FULL".equals(courseClass.getStatus()) && occupied < courseClass.getMaxStudents()) {
      courseClass.setStatus("OPEN");
      courseClassRepository.save(courseClass);
    }
    return true;
  }
}
