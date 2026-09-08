package com.ntt.language_center_management.dto.response;

public record StudentAttendanceSummaryResponse(
    Integer studentId,
    String studentCode,
    String studentName,
    long totalMarked,
    long present,
    long absent,
    long late,
    long excused,
    double attendanceRate) {}
