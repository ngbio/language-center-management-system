package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;

public record PopularCourseReportResponse(
    Integer courseId,
    String courseCode,
    String courseName,
    long registrations,
    long paidEnrollments,
    BigDecimal revenue) {}
