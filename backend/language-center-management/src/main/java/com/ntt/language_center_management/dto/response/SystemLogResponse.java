package com.ntt.language_center_management.dto.response;

import java.time.LocalDateTime;

public record SystemLogResponse(Long id, String level, String eventType, String message,
    String requestId, String actorEmail, String httpMethod, String requestPath,
    Integer httpStatus, LocalDateTime createdAt) {}
