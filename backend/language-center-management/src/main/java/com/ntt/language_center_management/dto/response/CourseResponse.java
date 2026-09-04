package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;

public record CourseResponse(
    Integer id,
    String courseCode,
    String courseName,
    String slug,
    String shortDescription,
    String description,
    String thumbnailUrl,
    String bannerUrl,
    String targetAudience,
    String prerequisites,
    String learningOutcomes,
    String syllabusSummary,
    String certificateInfo,
    BigDecimal tuitionFee,
    int totalSessions,
    Integer durationHours,
    String status,
    String publicationStatus,
    Date publishedAt,
    boolean featured,
    Integer levelId,
    String levelCode,
    String levelName,
    Integer languageId,
    String languageCode,
    String languageName,
    Date createdAt,
    Date updatedAt) {}
