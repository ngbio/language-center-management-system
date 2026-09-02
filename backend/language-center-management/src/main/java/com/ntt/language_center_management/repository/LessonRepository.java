package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Lesson;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRepository extends JpaRepository<Lesson, Integer> {

  List<Lesson> findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(
      Integer courseClassId);

  boolean existsByClassScheduleId_Id(Integer scheduleId);

  boolean existsByClassScheduleId_IdAndLessonDate(Integer scheduleId, Date lessonDate);

  boolean existsByClassScheduleId_IdAndLessonDateAndIdNot(
      Integer scheduleId, Date lessonDate, Integer id);

  long countByClassScheduleId_CourseClassId_Id(Integer courseClassId);

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
}
