package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.AccountStatus;

import java.util.Date;

public record UserResponse(
    Integer id,
    String username,
    String fullName,
    String email,
    String phoneNumber,
    String address,
    String roleName,
    String roleCode,
    AccountStatus status,
    Date createdAt,
    Date updatedAt) {}
