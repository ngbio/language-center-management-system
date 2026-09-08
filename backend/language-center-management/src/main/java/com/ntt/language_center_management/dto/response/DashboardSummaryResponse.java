package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
    long totalStudents,
    long totalTeachers,
    long totalCourses,
    long activeClasses,
    long upcomingClasses,
    long pendingEnrollments,
    long paidEnrollments,
    BigDecimal grossRevenue,
    BigDecimal refundedAmount,
    BigDecimal netRevenue) {}
