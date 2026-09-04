package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.CourseContent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseContentRepository extends JpaRepository<CourseContent, Integer> {

  List<CourseContent> findBySectionId_IdAndPublicationStatusOrderByDisplayOrderAsc(
      Integer sectionId, String publicationStatus);

  List<CourseContent>
      findBySectionId_IdAndPublicationStatusAndIsPreviewTrueOrderByDisplayOrderAsc(
          Integer sectionId, String publicationStatus);
}
