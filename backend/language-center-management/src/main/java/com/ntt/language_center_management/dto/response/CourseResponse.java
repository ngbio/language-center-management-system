package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.PublicationStatus;

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
    CatalogStatus status,
    PublicationStatus publicationStatus,
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
