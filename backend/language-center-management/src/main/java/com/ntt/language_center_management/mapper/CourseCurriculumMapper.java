package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.response.CourseContentResponse;
import com.ntt.language_center_management.dto.response.CourseSectionResponse;
import com.ntt.language_center_management.entity.CourseContent;
import com.ntt.language_center_management.entity.CourseSection;
import org.springframework.stereotype.Component;

@Component
public class CourseCurriculumMapper {

  public CourseSectionResponse toSectionResponse(CourseSection section) {
    return new CourseSectionResponse(
        section.getId(),
        section.getTitle(),
        section.getDescription(),
        section.getDisplayOrder());
  }

  public CourseContentResponse toContentResponse(CourseContent content) {
    return new CourseContentResponse(
        content.getId(),
        content.getTitle(),
        content.getSummary(),
        content.getContentHtml(),
        content.getAudioUrl(),
        content.getVideoUrl(),
        content.getDocumentUrl(),
        content.getContentType(),
        content.getDisplayOrder(),
        content.getIsPreview());
  }
}
