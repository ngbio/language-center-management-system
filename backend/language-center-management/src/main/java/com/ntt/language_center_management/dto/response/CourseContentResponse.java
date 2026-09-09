package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.CourseContentType;

public record CourseContentResponse(
    Integer id,
    String title,
    String summary,
    String contentHtml,
    String audioUrl,
    String videoUrl,
    String documentUrl,
    CourseContentType contentType,
    int displayOrder,
    boolean preview) {}
