package com.ntt.language_center_management.dto.response;

import java.util.List;

public record ClassAttendanceSummaryResponse(
    Integer classId,
    String classCode,
    String className,
    long totalLessons,
    long completedLessons,
    List<StudentAttendanceSummaryResponse> students) {}
