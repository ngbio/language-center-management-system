package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegisterRequest(
        @NotBlank @Size(max = 100) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 150) String email,
        @Size(max = 20) String phoneNumber,
        @Size(max = 255) String address) {
}
