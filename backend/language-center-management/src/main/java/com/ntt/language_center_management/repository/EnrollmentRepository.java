package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Date;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Integer> {

  long countByEnrollmentStatusAndPaymentStatus(String enrollmentStatus, String paymentStatus);

  long countByCourseClassId_IdAndEnrollmentStatusIn(
      Integer courseClassId, Collection<String> statuses);

  boolean existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusIn(
      Integer studentId, Integer courseClassId, Collection<String> statuses);

  boolean existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusAndPaymentStatus(
      Integer studentId,
      Integer courseClassId,
      String enrollmentStatus,
      String paymentStatus);

  boolean existsByStudentId_IdAndCourseClassId_Id(Integer studentId, Integer courseClassId);

  @Query(
      """
      select count(e) > 0 from Enrollment e
      join e.courseClassId courseClass
      where e.studentId.id = :studentId
        and courseClass.courseId.id = :courseId
        and e.enrollmentStatus = 'CONFIRMED'
        and e.paymentStatus = 'PAID'
      """)
  boolean existsPaidConfirmedAccess(
      @Param("studentId") Integer studentId, @Param("courseId") Integer courseId);

  @Query(
      """
      select distinct course from Enrollment e
      join e.courseClassId courseClass
      join courseClass.courseId course
      join fetch course.levelId level
      join fetch level.languageId
      where e.studentId.id = :studentId
        and e.enrollmentStatus = 'CONFIRMED'
        and e.paymentStatus = 'PAID'
        and course.status = 'ACTIVE'
        and course.publicationStatus = 'PUBLISHED'
      order by course.courseName asc
      """)
  List<Course> findAccessibleCoursesByStudentId(@Param("studentId") Integer studentId);

  @Query(
      """
      select distinct courseClass from Enrollment e
      join e.courseClassId courseClass
      join courseClass.courseId course
      where e.studentId.id = :studentId
        and e.enrollmentStatus = 'CONFIRMED'
        and e.paymentStatus = 'PAID'
        and courseClass.status <> 'CANCELLED'
        and course.status = 'ACTIVE'
        and course.publicationStatus = 'PUBLISHED'
      order by courseClass.startDate desc
      """)
  List<Courseclass> findAccessibleClassesByStudentId(@Param("studentId") Integer studentId);

  List<Enrollment> findByStudentId_IdOrderByEnrollmentDateDesc(Integer studentId);

  List<Enrollment> findByCourseClassId_IdOrderByEnrollmentDateDesc(Integer courseClassId);

  List<Enrollment>
      findByCourseClassId_IdAndEnrollmentStatusAndPaymentStatusOrderByStudentId_UserId_FullNameAsc(
          Integer courseClassId, String enrollmentStatus, String paymentStatus);

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

  @Query("select e.id from Enrollment e where e.enrollmentStatus = 'CONFIRMED' and e.paymentStatus = 'PENDING' and e.paymentDeadline < :now")
  List<Integer> findExpiredPendingIds(@Param("now") Date now);

  @Query(value = """
      SELECT YEAR(enrollment_date), MONTH(enrollment_date), COUNT(*),
             SUM(enrollment_status = 'CONFIRMED'),
             SUM(payment_status = 'PAID'),
             SUM(enrollment_status = 'CANCELLED')
      FROM enrollment
      WHERE enrollment_date >= :from AND enrollment_date < :toExclusive
      GROUP BY YEAR(enrollment_date), MONTH(enrollment_date)
      ORDER BY YEAR(enrollment_date), MONTH(enrollment_date)
      """, nativeQuery = true)
  List<Object[]> aggregateByMonth(
      @Param("from") Date from, @Param("toExclusive") Date toExclusive);

  @Query(value = """
      SELECT c.id, c.course_code, c.course_name, COUNT(e.id),
             SUM(e.enrollment_status = 'CONFIRMED' AND e.payment_status = 'PAID'),
             COALESCE(SUM(CASE WHEN e.enrollment_status = 'CONFIRMED'
                                   AND e.payment_status = 'PAID'
                               THEN e.amount_due ELSE 0 END), 0)
      FROM enrollment e
      JOIN courseclass cc ON cc.id = e.course_class_id
      JOIN course c ON c.id = cc.course_id
      WHERE e.enrollment_date >= :from AND e.enrollment_date < :toExclusive
      GROUP BY c.id, c.course_code, c.course_name
      ORDER BY 5 DESC, 4 DESC, c.course_name ASC
      LIMIT :limit
      """, nativeQuery = true)
  List<Object[]> findPopularCourses(
      @Param("from") Date from,
      @Param("toExclusive") Date toExclusive,
      @Param("limit") int limit);
}
