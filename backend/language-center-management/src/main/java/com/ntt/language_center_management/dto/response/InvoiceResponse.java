package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public record InvoiceResponse(
    String invoiceNumber, Integer enrollmentId, String studentCode, String studentName,
    String studentEmail, String courseCode, String courseName, String classCode,
    String className, BigDecimal tuitionAmount, BigDecimal paidAmount,
    BigDecimal refundedAmount, BigDecimal netPaidAmount, String enrollmentStatus,
    String paymentStatus, Date issuedAt, List<PaymentResponse> payments,
    List<RefundResponse> refunds) {}
