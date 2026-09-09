package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.CatalogStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeCatalogStatusRequest(
    @NotNull(message = "Trạng thái không được để trống") CatalogStatus status) {}
