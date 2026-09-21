package com.ntt.language_center_management.payment;

public record PaymentCommand(Integer enrollmentId, Integer studentId, String classCode, long amount) {}
