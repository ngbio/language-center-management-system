package com.ntt.language_center_management.dto.response;

public record FirebaseChatTokenResponse(
    String customToken,
    String uid,
    Integer userId,
    String fullName,
    String role,
    String consultantUid,
    String consultantName) {}
