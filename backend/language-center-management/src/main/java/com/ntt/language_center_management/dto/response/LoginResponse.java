package com.ntt.language_center_management.dto.response;

public record LoginResponse(
        String token,
        Integer userId,
        String email,
        String role) {
}
