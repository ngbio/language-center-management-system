package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateEnrollmentRequest(
    @NotNull(message = "Lớp học không được để trống") Integer courseClassId,
    Integer studentId) {}
