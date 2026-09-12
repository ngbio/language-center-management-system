package com.ntt.language_center_management.event;

public record PasswordResetCompletedMailEvent(String email, String fullName) {}
