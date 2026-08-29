package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeCatalogStatusRequest(
    @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(
            regexp = "ACTIVE|INACTIVE",
            message = "Trạng thái phải là ACTIVE hoặc INACTIVE")
        String status) {}
