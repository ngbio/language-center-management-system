package com.ntt.language_center_management.dto.response;

import java.util.Date;

public record StudentProfileResponse(
    Integer studentId,
    String studentCode,
    Integer userId,
    String username,
    String fullName,
    String email,
    String phoneNumber,
    String address,
    Date dateOfBirth,
    String gender,
    String avatar,
    String status,
    Date createdAt,
    Date updatedAt) {}
