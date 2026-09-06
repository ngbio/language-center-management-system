package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;

public record EnrollmentSummaryResponse(
    Integer id,
    Integer studentId,
    String studentCode,
    String studentName,
    Integer courseClassId,
    String classCode,
    String className,
    Integer courseId,
    String courseCode,
    String courseName,
    Date enrollmentDate,
    Date paymentDeadline,
    BigDecimal amountDue,
    String enrollmentStatus,
    String paymentStatus) {}
