package com.ntt.language_center_management.event;

public record PasswordResetRequestedMailEvent(
    String email, String fullName, String resetUrl, int expirationMinutes) {}
