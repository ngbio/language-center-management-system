package com.ntt.language_center_management.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

public record ClassScheduleRequest(
    Integer roomId,
    @Min(value = 1, message = "Ngày trong tuần phải từ 1 đến 7")
        @Max(value = 7, message = "Ngày trong tuần phải từ 1 đến 7")
        short dayOfWeek,
    @NotNull(message = "Giờ bắt đầu không được để trống")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
    @NotNull(message = "Giờ kết thúc không được để trống")
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
    @NotBlank(message = "Hình thức học không được để trống")
        @Pattern(
            regexp = "IN_PERSON|ONLINE",
            message = "Hình thức học phải là IN_PERSON hoặc ONLINE")
        String deliveryMode,
    @Size(max = 500, message = "Đường dẫn phòng học không được vượt quá 500 ký tự")
        String meetingUrl) {}
