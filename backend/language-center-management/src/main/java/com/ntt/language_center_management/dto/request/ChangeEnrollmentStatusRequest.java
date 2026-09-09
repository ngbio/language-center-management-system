package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeEnrollmentStatusRequest(
    @NotNull(message = "Trạng thái đăng ký không được để trống") EnrollmentStatus status) {}
