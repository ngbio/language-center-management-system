package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.PublicationStatus;
import jakarta.validation.constraints.NotNull;

public record PublicationStatusRequest(@NotNull PublicationStatus status) {}
