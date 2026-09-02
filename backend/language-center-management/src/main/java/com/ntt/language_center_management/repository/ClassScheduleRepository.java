package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Classschedule;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassScheduleRepository extends JpaRepository<Classschedule, Integer> {

  List<Classschedule> findByCourseClassId_Id(Integer courseClassId);

  List<Classschedule> findByCourseClassId_IdOrderByDayOfWeekAscStartTimeAsc(
      Integer courseClassId);

  boolean existsByCourseClassId_IdAndDayOfWeekAndStartTime(
      Integer courseClassId, short dayOfWeek, Date startTime);

  @Query(
      """
      select count(s) > 0 from Classschedule s
      join s.courseClassId c
      where (:scheduleId is null or s.id <> :scheduleId)
        and c.status <> 'CANCELLED'
        and c.startDate <= :endDate
        and c.endDate >= :startDate
        and s.dayOfWeek = :dayOfWeek
        and s.startTime < :endTime
        and s.endTime > :startTime
        and (
          (:roomId is not null and s.roomId.id = :roomId)
          or (:teacherId is not null and c.teacherId.id = :teacherId)
        )
      """)
  boolean existsResourceConflict(
      @Param("scheduleId") Integer scheduleId,
      @Param("roomId") Integer roomId,
      @Param("teacherId") Integer teacherId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate,
      @Param("dayOfWeek") short dayOfWeek,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);

  @Query(
      """
      select count(s) > 0 from Classschedule s
      where s.courseClassId.id = :classId
        and (:scheduleId is null or s.id <> :scheduleId)
        and s.dayOfWeek = :dayOfWeek
        and s.startTime < :endTime
        and s.endTime > :startTime
      """)
  boolean existsClassTimeConflict(
      @Param("scheduleId") Integer scheduleId,
      @Param("classId") Integer classId,
      @Param("dayOfWeek") short dayOfWeek,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);

  @Query(
      """
      select count(s) > 0 from Classschedule s
      join s.courseClassId c
      where c.id <> :classId
        and c.status <> 'CANCELLED'
        and c.startDate <= :endDate
        and c.endDate >= :startDate
        and s.dayOfWeek = :dayOfWeek
        and s.startTime < :endTime
        and s.endTime > :startTime
        and (
          (:roomId is not null and s.roomId.id = :roomId)
          or (:teacherId is not null and c.teacherId.id = :teacherId)
        )
      """)
  boolean existsConflict(
      @Param("classId") Integer classId,
      @Param("roomId") Integer roomId,
      @Param("teacherId") Integer teacherId,
      @Param("startDate") Date startDate,
      @Param("endDate") Date endDate,
      @Param("dayOfWeek") short dayOfWeek,
      @Param("startTime") Date startTime,
      @Param("endTime") Date endTime);
}
