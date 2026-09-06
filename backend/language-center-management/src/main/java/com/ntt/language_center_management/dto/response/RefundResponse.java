package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;

public record RefundResponse(
    Integer id, Integer enrollmentId, Integer paymentId, String refundCode,
    String paymentMethod, String studentName, String className,
    BigDecimal amount, String status, String gatewayRefundId, String errorMessage,
    String reason, Integer processedById,
    String processedByName, Date createdAt, Date completedAt) {}
