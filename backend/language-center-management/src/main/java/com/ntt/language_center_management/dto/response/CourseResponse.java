package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;

public record CourseResponse(
    Integer id,
    String courseCode,
    String courseName,
    String description,
    BigDecimal tuitionFee,
    int totalSessions,
    Integer durationHours,
    String status,
    Integer levelId,
    String levelCode,
    String levelName,
    Integer languageId,
    String languageCode,
    String languageName,
    Date createdAt,
    Date updatedAt) {}
