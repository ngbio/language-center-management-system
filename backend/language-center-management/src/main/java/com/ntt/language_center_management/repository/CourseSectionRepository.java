package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.CourseSection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseSectionRepository extends JpaRepository<CourseSection, Integer> {

  List<CourseSection> findByCourseId_IdOrderByDisplayOrderAsc(Integer courseId);

  Optional<CourseSection> findByIdAndCourseId_StatusAndCourseId_PublicationStatus(
      Integer id, String courseStatus, String publicationStatus);
}
