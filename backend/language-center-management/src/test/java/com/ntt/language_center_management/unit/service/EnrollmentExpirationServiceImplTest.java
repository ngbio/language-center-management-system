package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.ntt.language_center_management.unit.fixture.TestFixtures.courseClass;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.service.impl.EnrollmentExpirationServiceImpl;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrollmentExpirationServiceImplTest {
  private EnrollmentRepository enrollmentRepository;
  private CourseClassRepository classRepository;
  private EnrollmentExpirationServiceImpl service;

  @BeforeEach
  void setUp() {
    enrollmentRepository = mock(EnrollmentRepository.class);
    classRepository = mock(CourseClassRepository.class);
    service = new EnrollmentExpirationServiceImpl(enrollmentRepository, classRepository);
  }

  @Test
  void shouldCancelEnrollmentAndReopenClassWhenDeadlineIsOverdue() {
    Courseclass courseClass = courseClass(5);
    courseClass.setStatus(ClassStatus.FULL);
    courseClass.setMaxStudents(20);
    Enrollment enrollment = enrollment(courseClass, new Date(System.currentTimeMillis() - 60_000));
    when(enrollmentRepository.lockById(10)).thenReturn(Optional.of(enrollment));
    when(enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(eq(5), anySet()))
        .thenReturn(19L);

    assertTrue(service.expireIfOverdue(10));
    assertTrue(enrollment.getEnrollmentStatus() == EnrollmentStatus.CANCELLED);
    assertTrue(enrollment.getPaymentStatus() == EnrollmentPaymentStatus.CANCELLED);
    assertTrue(courseClass.getStatus() == ClassStatus.OPEN);
    verify(enrollmentRepository).saveAndFlush(enrollment);
    verify(classRepository).save(courseClass);
  }

  @Test
  void shouldKeepEnrollmentWhenDeadlineIsInFuture() {
    Courseclass courseClass = courseClass(5);
    Enrollment enrollment = enrollment(courseClass, new Date(System.currentTimeMillis() + 60_000));
    when(enrollmentRepository.lockById(10)).thenReturn(Optional.of(enrollment));

    assertFalse(service.expireIfOverdue(10));
    verify(enrollmentRepository, never()).saveAndFlush(enrollment);
  }

  @Test
  void shouldReturnFalseWhenEnrollmentDoesNotExist() {
    when(enrollmentRepository.lockById(10)).thenReturn(Optional.empty());
    assertFalse(service.expireIfOverdue(10));
  }

  private Enrollment enrollment(Courseclass courseClass, Date deadline) {
    Enrollment enrollment =
        com.ntt.language_center_management.unit.fixture.TestFixtures.enrollment(10);
    enrollment.setCourseClassId(courseClass);
    enrollment.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PENDING);
    enrollment.setPaymentDeadline(deadline);
    return enrollment;
  }
}
