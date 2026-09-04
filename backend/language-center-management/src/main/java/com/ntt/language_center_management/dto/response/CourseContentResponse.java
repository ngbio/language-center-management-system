package com.ntt.language_center_management.dto.response;

public record CourseContentResponse(
    Integer id,
    String title,
    String summary,
    String contentHtml,
    String audioUrl,
    String videoUrl,
    String documentUrl,
    String contentType,
    int displayOrder,
    boolean preview) {}
