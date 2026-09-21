package com.ntt.language_center_management.payment;

/** Provider-verified callback; amount parsing happens only for successful payments. */
public record PaymentCallback(String transactionCode, boolean succeeded, String amount,
    String referenceCode, String errorMessage) {}
