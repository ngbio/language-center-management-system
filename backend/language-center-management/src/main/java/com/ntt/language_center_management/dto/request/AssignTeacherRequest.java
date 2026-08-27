package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotNull;

public record AssignTeacherRequest(@NotNull Integer teacherId) {}
