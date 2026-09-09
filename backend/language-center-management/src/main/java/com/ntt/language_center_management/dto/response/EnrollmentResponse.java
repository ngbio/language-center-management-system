package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;

import java.math.BigDecimal;
import java.util.Date;

public record EnrollmentResponse(
    Integer id,
    Date enrollmentDate,
    Date paymentDeadline,
    BigDecimal amountDue,
    EnrollmentStatus enrollmentStatus,
    EnrollmentPaymentStatus paymentStatus,
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
