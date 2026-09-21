package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.service.EnrollmentExpirationService;
import com.ntt.language_center_management.service.EnrollmentLifecycle;
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
  private final EnrollmentLifecycle lifecycle;
  private final java.time.Clock clock;

  public EnrollmentExpirationServiceImpl(EnrollmentRepository enrollmentRepository,
      CourseClassRepository courseClassRepository, EnrollmentLifecycle lifecycle, java.time.Clock clock) {
    this.enrollmentRepository = enrollmentRepository;
    this.courseClassRepository = courseClassRepository;
    this.lifecycle = lifecycle;
    this.clock = clock;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean expireIfOverdue(Integer enrollmentId) {
    Enrollment enrollment = enrollmentRepository.lockById(enrollmentId).orElse(null);
    return enrollment != null && expire(enrollment, Date.from(clock.instant()));
  }

  @Override
  @Scheduled(fixedDelayString = "${app.enrollment.expiration-check-ms:60000}")
  @Transactional
  public void expireOverdueEnrollments() {
    Date now = Date.from(clock.instant());
    for (Integer id : enrollmentRepository.findExpiredPendingIds(now)) {
      enrollmentRepository.lockById(id).ifPresent(enrollment -> expire(enrollment, now));
    }
  }

  private boolean expire(Enrollment enrollment, Date now) {
    if (!lifecycle.expire(enrollment, now)) return false;
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
