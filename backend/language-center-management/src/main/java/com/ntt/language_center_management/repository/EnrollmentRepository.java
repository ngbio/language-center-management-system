package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Enrollment;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

  long countByCourseClassId_IdAndEnrollmentStatusIn(
      Integer courseClassId, Collection<String> statuses);

  boolean existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusIn(
      Integer studentId, Integer courseClassId, Collection<String> statuses);
}
