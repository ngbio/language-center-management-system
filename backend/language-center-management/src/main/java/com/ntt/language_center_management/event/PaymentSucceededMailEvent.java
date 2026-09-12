package com.ntt.language_center_management.event;

import java.math.BigDecimal;

public record PaymentSucceededMailEvent(
    String email,
    String fullName,
    String transactionCode,
    String className,
    BigDecimal amount) {}
