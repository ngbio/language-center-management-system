package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;

public record EnrollmentResponse(
    Integer id,
    Date enrollmentDate,
    BigDecimal amountDue,
    String enrollmentStatus,
    String paymentStatus,
    Date confirmedAt,
    Date cancelledAt,
    String cancellationReason,
    Integer studentId,
    String studentCode,
    String studentName,
    String studentEmail,
    Integer courseClassId,
    String classCode,
    String className,
    Integer courseId,
    String courseCode,
    String courseName) {}
