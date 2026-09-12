package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminStaffAccountRequest(
    @NotBlank @Size(max = 100) String username,
    @NotBlank
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,100}$",
            message = "Mật khẩu phải có 8-100 ký tự, gồm chữ thường, chữ số và ký tự đặc biệt")
        String password,
    @NotBlank @Size(max = 150) String fullName,
    @NotBlank @Email @Size(max = 150) String email,
    @Pattern(
            regexp = "^$|^0[0-9]{9}$",
            message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
        String phoneNumber,
    @Size(max = 255) String address,
    @NotBlank
        @Pattern(
            regexp = "ADMIN|CONSULTANT",
            message = "Vai trò chỉ được là ADMIN hoặc CONSULTANT")
        String roleCode) {}
