package com.ntt.language_center_management.dto.response;

public record TeacherOptionResponse(
    Integer id,
    String teacherCode,
    String fullName,
    String specialization,
    String degree) {}
