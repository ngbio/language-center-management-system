package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.PublicationStatus;

import com.ntt.language_center_management.entity.Course;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CourseRepository
    extends JpaRepository<Course, Integer>, JpaSpecificationExecutor<Course> {

  Optional<Course> findByCourseCodeIgnoreCase(String courseCode);

  Optional<Course> findBySlugAndStatusAndPublicationStatus(
      String slug, CatalogStatus status,
      PublicationStatus publicationStatus);

  Optional<Course> findByIdAndStatus(
      Integer id, CatalogStatus status);

  boolean existsByCourseCodeIgnoreCase(String courseCode);

  boolean existsByCourseCodeIgnoreCaseAndIdNot(String courseCode, Integer id);

  boolean existsBySlugIgnoreCase(String slug);

  boolean existsBySlugIgnoreCaseAndIdNot(String slug, Integer id);
}
