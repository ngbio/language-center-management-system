package com.ntt.language_center_management.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record LessonRescheduleRequest(
    @NotNull(message = "Ngày học mới không được để trống")
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate lessonDate,
    @NotBlank(message = "Lý do dời lịch không được để trống")
        @Size(max = 500, message = "Lý do dời lịch không được vượt quá 500 ký tự")
        String reason) {}
