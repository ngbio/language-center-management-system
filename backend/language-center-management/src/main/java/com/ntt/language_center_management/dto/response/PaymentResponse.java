package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;

public record PaymentResponse(
    Integer id, Integer enrollmentId, String transactionCode, String method,
    BigDecimal amount, String status, String paymentUrl, Date createdAt, Date completedAt) {}
