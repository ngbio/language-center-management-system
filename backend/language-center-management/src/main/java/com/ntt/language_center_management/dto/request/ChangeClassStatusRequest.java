package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ChangeClassStatusRequest(
    @NotNull @Pattern(regexp = "DRAFT|OPEN|FULL|IN_PROGRESS|COMPLETED|CANCELLED") String status) {}
