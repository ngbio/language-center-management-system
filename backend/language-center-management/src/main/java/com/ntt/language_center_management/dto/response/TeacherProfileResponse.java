package com.ntt.language_center_management.dto.response;

import java.util.Date;

public record TeacherProfileResponse(
    Integer teacherId,
    String teacherCode,
    Integer userId,
    String username,
    String fullName,
    String email,
    String phoneNumber,
    String address,
    String specialization,
    String degree,
    int experienceYears,
    String status,
    Date createdAt,
    Date updatedAt) {}
