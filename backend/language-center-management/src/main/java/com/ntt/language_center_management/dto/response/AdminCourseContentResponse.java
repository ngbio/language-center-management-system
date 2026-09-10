package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.CourseContentType;
import com.ntt.language_center_management.enums.PublicationStatus;

public record AdminCourseContentResponse(
    Integer id, Integer sectionId, String title, String summary, String contentHtml,
    String audioUrl, String videoUrl, String documentUrl, CourseContentType contentType,
    int displayOrder, boolean preview, PublicationStatus publicationStatus) {}
