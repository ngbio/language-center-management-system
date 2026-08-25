package com.ntt.language_center_management.dto.response;

public record LevelResponse(
    Integer id,
    String levelCode,
    String levelName,
    String description,
    int displayOrder,
    String status,
    Integer languageId,
    String languageCode,
    String languageName) {}
