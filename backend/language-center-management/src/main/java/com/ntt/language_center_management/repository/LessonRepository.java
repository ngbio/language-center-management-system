package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.enums.LessonStatus;

import com.ntt.language_center_management.entity.Lesson;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select lesson from Lesson lesson where lesson.id = :id")
  Optional<Lesson> lockById(@Param("id") Integer id);

  List<Lesson> findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(
      Integer courseClassId);

  @Query(
      """
      select lesson from Lesson lesson
      join lesson.classScheduleId schedule
      join schedule.courseClassId courseClass
      where exists (
        select enrollment.id from Enrollment enrollment
        where enrollment.courseClassId.id = courseClass.id
          and lower(enrollment.studentId.userId.email) = lower(:email)
          and enrollment.enrollmentStatus = 'CONFIRMED'
          and enrollment.paymentStatus = 'PAID'
      )
        and courseClass.status <> 'CANCELLED'
      order by lesson.lessonDate asc, schedule.startTime asc
      """)
  List<Lesson> findAccessibleLessonsByStudentEmail(@Param("email") String email);

  boolean existsByClassScheduleId_Id(Integer scheduleId);

  boolean existsByClassScheduleId_IdAndLessonDate(Integer scheduleId, Date lessonDate);

  boolean existsByClassScheduleId_IdAndLessonDateAndIdNot(
      Integer scheduleId, Date lessonDate, Integer id);

  long countByClassScheduleId_CourseClassId_Id(Integer courseClassId);

  long countByClassScheduleId_CourseClassId_IdAndStatusNot(
      Integer courseClassId, LessonStatus status);

  long countByClassScheduleId_CourseClassId_IdAndStatus(
      Integer courseClassId, LessonStatus status);

  List<Lesson> findByStatusAndLessonDateLessThanEqual(
      LessonStatus status, Date lessonDate);

  List<Lesson> findByStatusInAndLessonDateLessThanEqual(
      Collection<LessonStatus> statuses, Date lessonDate);

  @Query(
      """
      select count(l) > 0 from Lesson l
      join l.classScheduleId s
      join s.courseClassId c
      where (:lessonId is null or l.id <> :lessonId)
        and c.id <> :classId
        and l.status <> 'CANCELLED'
        and c.status <> 'CANCELLED'
        and l.lessonDate = :lessonDate
        and s.startTime < :endTime
        and s.endTime > :startTime
        and (
          (:roomId is not null and s.roomId.id = :roomId)
          or (:teacherId is not null and c.teacherId.id = :teacherId)
        )
      """)
  boolean existsResourceConflictOnDate(
      @Param("lessonId") Integer lessonId,
      @Param("classId") Integer classId,
      @Param("roomId") Integer roomId,
      @Param("teacherId") Integer teacherId,
      @Param("lessonDate") Date lessonDate,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);

  @Query(value = """
      SELECT t.id, t.teacher_code, u.full_name,
             COUNT(DISTINCT CASE WHEN cc.status <> 'CANCELLED'
                                      AND cc.start_date <= :toDate
                                      AND cc.end_date >= :fromDate THEN cc.id END),
             COUNT(DISTINCT CASE WHEN l.lesson_date >= :fromDate
                                      AND l.lesson_date <= :toDate
                                      AND l.status <> 'CANCELLED' THEN l.id END),
             COUNT(DISTINCT CASE WHEN l.lesson_date >= :fromDate
                                      AND l.lesson_date <= :toDate
                                      AND l.status = 'COMPLETED' THEN l.id END)
      FROM teacher t
      JOIN `user` u ON u.id = t.user_id
      LEFT JOIN courseclass cc ON cc.teacher_id = t.id
      LEFT JOIN classschedule cs ON cs.course_class_id = cc.id
      LEFT JOIN lesson l ON l.class_schedule_id = cs.id
      WHERE u.status = 'ACTIVE'
      GROUP BY t.id, t.teacher_code, u.full_name
      ORDER BY 5 DESC, u.full_name ASC
      """, nativeQuery = true)
  List<Object[]> aggregateTeacherLoad(
      @Param("fromDate") Date fromDate, @Param("toDate") Date toDate);
}
