package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Date;

public record StudentProfileUpdateRequest(
    @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên không được vượt quá 150 ký tự")
        String fullName,
    @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự") String phoneNumber,
    @Size(max = 255, message = "Địa chỉ không được vượt quá 255 ký tự") String address,
    @Past(message = "Ngày sinh phải là ngày trong quá khứ") Date dateOfBirth,
    @Pattern(
            regexp = "^(MALE|FEMALE|OTHER)?$",
            message = "Giới tính phải là MALE, FEMALE hoặc OTHER")
        String gender,
    @Size(max = 500, message = "Đường dẫn ảnh không được vượt quá 500 ký tự") String avatar) {}
