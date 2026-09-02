package com.ntt.language_center_management.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LessonRescheduleRequest(
    @NotNull(message = "Ngày học mới không được để trống")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate lessonDate) {}
