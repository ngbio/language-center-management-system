package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.NotificationType;
import java.util.Date;

public record NotificationResponse(
    Integer id,
    String title,
    String content,
    NotificationType notificationType,
    boolean read,
    Date createdAt,
    Date readAt) {}
