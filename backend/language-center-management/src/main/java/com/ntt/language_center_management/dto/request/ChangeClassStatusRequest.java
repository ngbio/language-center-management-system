package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.ClassStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeClassStatusRequest(
    @NotNull ClassStatus status) {}
