package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Enrollment;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

  long countByCourseClassId_IdAndEnrollmentStatusIn(
      Integer courseClassId, Collection<String> statuses);

  boolean existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusIn(
      Integer studentId, Integer courseClassId, Collection<String> statuses);

  boolean existsByStudentId_IdAndCourseClassId_Id(Integer studentId, Integer courseClassId);

  List<Enrollment> findByStudentId_IdOrderByEnrollmentDateDesc(Integer studentId);

  List<Enrollment> findByCourseClassId_IdOrderByEnrollmentDateDesc(Integer courseClassId);

  @Query(
      """
      select count(e) > 0 from Enrollment e
      join e.courseClassId currentClass
      join currentClass.classscheduleList currentSchedule
      where e.studentId.id = :studentId
        and e.enrollmentStatus in :statuses
        and currentClass.id <> :targetClassId
        and exists (
          select targetSchedule.id from Classschedule targetSchedule
          join targetSchedule.courseClassId targetClass
          where targetClass.id = :targetClassId
            and currentClass.startDate <= targetClass.endDate
            and currentClass.endDate >= targetClass.startDate
            and currentSchedule.dayOfWeek = targetSchedule.dayOfWeek
            and currentSchedule.startTime < targetSchedule.endTime
            and currentSchedule.endTime > targetSchedule.startTime
        )
      """)
  boolean existsScheduleConflict(
      @Param("studentId") Integer studentId,
      @Param("targetClassId") Integer targetClassId,
      @Param("statuses") Collection<String> statuses);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select e from Enrollment e where e.id = :id")
  Optional<Enrollment> lockById(@Param("id") Integer id);
}
