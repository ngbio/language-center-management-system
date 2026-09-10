package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseSectionRequest(
    @NotBlank @Size(max = 255) String title,
    @Size(max = 65535) String description) {}
