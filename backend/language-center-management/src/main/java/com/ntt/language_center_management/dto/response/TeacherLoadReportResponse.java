package com.ntt.language_center_management.dto.response;

public record TeacherLoadReportResponse(
    Integer teacherId,
    String teacherCode,
    String teacherName,
    long classes,
    long lessons,
    long completedLessons) {}
