package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeUserStatusRequest(
    @NotNull(message = "Trạng thái không được để trống") AccountStatus status) {}
