package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.CourseContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CourseContentRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 65535) String summary,
    String contentHtml,
    @Size(max = 500) String audioUrl,
    @Size(max = 500) String videoUrl,
    @Size(max = 500) String documentUrl,
    @NotNull CourseContentType contentType,
    boolean preview) {}
