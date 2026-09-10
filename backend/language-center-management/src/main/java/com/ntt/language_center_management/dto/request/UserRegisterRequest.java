package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Date;

public record UserRegisterRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank(message = "Mật khẩu không được để trống")
        @Pattern(
            regexp = "^(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z\\d\\s]).{8,100}$",
            message = "Mật khẩu phải có 8-100 ký tự, gồm chữ thường, chữ số và ký tự đặc biệt")
        String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank(message = "Email không được để trống")
        @Email(message = "Email không đúng định dạng")
        @Pattern(
            regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
            message = "Email phải có dạng ten@mien.com")
        @Size(max = 150) String email,
        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng 0")
        String phoneNumber,
        @Size(max = 255) String address,
        @Past Date dateOfBirth,
        Gender gender,
        @Size(max = 500) String avatar) {
}
