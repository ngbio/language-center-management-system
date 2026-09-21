package com.ntt.language_center_management.event;

import java.math.BigDecimal;

/** Snapshot shared by notification channels; contains no managed JPA entities. */
public record PaymentSucceededEvent(
    Integer userId,
    String email,
    String fullName,
    String transactionCode,
    String className,
    BigDecimal amount) {}
