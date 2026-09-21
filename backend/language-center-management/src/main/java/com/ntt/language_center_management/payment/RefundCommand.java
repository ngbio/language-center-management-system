package com.ntt.language_center_management.payment;

public record RefundCommand(String refundCode, String idempotencyKey,
    String referenceCode, java.math.BigDecimal amount, String reason) {}
