package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.AttendanceStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AttendanceItemRequest(
    @NotNull Integer studentId,
    @NotNull AttendanceStatus status,
    @Size(max = 500) String note) {}
