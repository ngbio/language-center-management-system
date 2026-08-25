package com.ntt.language_center_management.dto.response;

public record LanguageResponse(
    Integer id, String languageCode, String languageName, String description, String status) {}
