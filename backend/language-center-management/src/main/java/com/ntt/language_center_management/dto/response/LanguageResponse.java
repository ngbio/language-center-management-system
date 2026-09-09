package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.CatalogStatus;

public record LanguageResponse(
    Integer id, String languageCode, String languageName, String description, CatalogStatus status) {}
