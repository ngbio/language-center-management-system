package com.ntt.language_center_management.dto.response;

import java.util.Date;

import com.ntt.language_center_management.enums.AccountStatus;

public record UserResponse(
        Integer id,
        String username,
        String fullName,
        String email,
        String phoneNumber,
        String address,
        String roleName,
        AccountStatus status,
        Date createdAt,
        Date updatedAt) {
}
