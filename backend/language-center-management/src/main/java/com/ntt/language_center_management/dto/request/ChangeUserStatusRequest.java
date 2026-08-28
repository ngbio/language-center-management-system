package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeUserStatusRequest(
    @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(
            regexp = "ACTIVE|INACTIVE|LOCKED",
            message = "Trạng thái phải là ACTIVE, INACTIVE hoặc LOCKED")
        String status) {}
