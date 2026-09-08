package com.ntt.language_center_management.dto.response;

public record ImageUploadResponse(
    String url,
    String publicId,
    String format,
    Integer width,
    Integer height,
    long bytes) {}
