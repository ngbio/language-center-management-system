package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Attendance;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

  boolean existsByLessonId_Id(Integer lessonId);

  List<Attendance> findByLessonId_IdOrderByEnrollmentId_StudentId_UserId_FullNameAsc(
      Integer lessonId);

  List<Attendance> findByEnrollmentId_StudentId_IdOrderByLessonId_LessonDateDesc(
      Integer studentId);

  @Query(
      """
      select attendance from Attendance attendance
      join attendance.enrollmentId enrollment
      join enrollment.studentId student
      join student.userId user
      join attendance.lessonId lesson
      where enrollment.courseClassId.id = :classId
      order by user.fullName asc, lesson.lessonDate asc
      """)
  List<Attendance> findByClassId(@Param("classId") Integer classId);

}
