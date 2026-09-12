package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResetPasswordRequest(
    @NotBlank(message = "Token không được để trống") String token,
    @NotBlank(message = "Mật khẩu mới không được để trống")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,100}$",
        message = "Mật khẩu mới phải có 8-100 ký tự, gồm chữ thường, chữ số và ký tự đặc biệt")
    String newPassword,
    @NotBlank(message = "Xác nhận mật khẩu không được để trống") String confirmPassword) {}
