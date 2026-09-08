package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpcomingClassReportResponse(
    Integer classId,
    String classCode,
    String className,
    Integer courseId,
    String courseName,
    String teacherName,
    LocalDate startDate,
    LocalDate endDate,
    int maxStudents,
    long reservedSeats,
    long availableSeats,
    BigDecimal tuitionFee,
    String status) {}
