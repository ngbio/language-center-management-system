package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TeacherProfileUpdateRequest(
    @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
        String fullName,
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự") String phoneNumber,
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự") String address,
    @Size(max = 150, message = "Chuyên môn không được vượt quá 150 ký tự") String specialization,
    @Size(max = 200, message = "Bằng cấp không được vượt quá 200 ký tự") String degree,
    @Min(value = 0, message = "Số năm kinh nghiệm không được âm")
        @Max(value = 80, message = "Số năm kinh nghiệm không được vượt quá 80")
        Integer experienceYears) {}
