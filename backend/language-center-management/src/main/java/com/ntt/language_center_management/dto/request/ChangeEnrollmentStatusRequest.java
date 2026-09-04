package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeEnrollmentStatusRequest(
    @NotBlank(message = "Trạng thái đăng ký không được để trống")
        @Pattern(
            regexp = "PENDING|CONFIRMED|CANCELLED",
            message = "Trạng thái phải là PENDING, CONFIRMED hoặc CANCELLED")
        String status) {}
