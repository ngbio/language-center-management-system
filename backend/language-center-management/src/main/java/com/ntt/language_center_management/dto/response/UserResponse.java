package com.ntt.language_center_management.dto.response;

import java.util.Date;

public record UserResponse(
    Integer id,
    String username,
    String fullName,
    String email,
    String phoneNumber,
    String address,
    String roleName,
    String status,
    Date createdAt,
    Date updatedAt) {}
