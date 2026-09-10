package com.ntt.language_center_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReorderRequest(@NotEmpty List<@Valid @NotNull Integer> ids) {}
