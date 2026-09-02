package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotNull;

public record TransferEnrollmentRequest(
    @NotNull(message = "Lớp học chuyển đến không được để trống") Integer targetCourseClassId) {}
