package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StaffCreateEnrollmentRequest(
    @NotNull(message = "Lớp học không được để trống") Integer courseClassId,
    @NotBlank(message = "Email học viên không được để trống")
        @Email(message = "Email học viên không đúng định dạng") String studentEmail) {}
