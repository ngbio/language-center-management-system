package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelEnrollmentRequest(
    @NotBlank(message = "Lý do hủy đăng ký không được để trống")
        @Size(max = 500, message = "Lý do hủy đăng ký không được vượt quá 500 ký tự")
        String cancellationReason) {}
