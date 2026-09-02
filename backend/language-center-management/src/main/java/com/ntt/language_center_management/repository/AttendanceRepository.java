package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Integer> {

  boolean existsByLessonId_Id(Integer lessonId);
}
