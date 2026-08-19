package com.ntt.language_center_management.dto.response;

public record ApiResponse<T>(
        int status,
        String message,
        T data) {
}
