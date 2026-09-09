package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.CatalogStatus;

public record LevelResponse(
    Integer id,
    String levelCode,
    String levelName,
    String description,
    int displayOrder,
    CatalogStatus status,
    Integer languageId,
    String languageCode,
    String languageName) {}
