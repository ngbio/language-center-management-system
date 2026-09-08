package com.ntt.language_center_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AttendanceBulkRequest(
    @NotEmpty(message = "Danh sách điểm danh không được để trống")
        List<@Valid AttendanceItemRequest> attendances) {}
