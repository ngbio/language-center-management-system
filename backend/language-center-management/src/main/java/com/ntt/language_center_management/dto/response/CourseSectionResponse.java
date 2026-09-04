package com.ntt.language_center_management.dto.response;

public record CourseSectionResponse(
    Integer id,
    String title,
    String description,
    int displayOrder) {}
