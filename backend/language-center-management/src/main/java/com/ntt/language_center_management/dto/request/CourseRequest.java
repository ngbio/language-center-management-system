package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class CourseRequest {
  private Integer id;

  @NotBlank
  @Size(max = 30)
  private String courseCode;

  @NotBlank
  @Size(max = 200)
  private String courseName;

  @NotBlank
  @Size(max = 220)
  @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*", message = "chỉ gồm chữ thường, số và dấu gạch ngang")
  private String slug;

  @Size(max = 500)
  private String shortDescription;

  @Size(max = 1000)
  private String description;

  @Size(max = 500)
  private String thumbnailUrl;

  @Size(max = 500)
  private String bannerUrl;

  private String targetAudience;
  private String prerequisites;
  private String learningOutcomes;
  private String syllabusSummary;

  @Size(max = 500)
  private String certificateInfo;

  @NotNull
  @DecimalMin("0.0")
  private BigDecimal tuitionFee;

  @Min(1)
  private int totalSessions;

  @Min(1)
  private Integer durationHours;

  @NotNull private Integer levelId;

  @NotBlank
  @Pattern(regexp = "ACTIVE|INACTIVE")
  private String status = "ACTIVE";

  @NotBlank
  @Pattern(regexp = "DRAFT|PUBLISHED|ARCHIVED")
  private String publicationStatus = "DRAFT";

  private boolean featured;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getCourseCode() {
    return courseCode;
  }

  public void setCourseCode(String courseCode) {
    this.courseCode = courseCode;
  }

  public String getCourseName() {
    return courseName;
  }

  public void setCourseName(String courseName) {
    this.courseName = courseName;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getShortDescription() {
    return shortDescription;
  }

  public void setShortDescription(String shortDescription) {
    this.shortDescription = shortDescription;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getThumbnailUrl() {
    return thumbnailUrl;
  }

  public void setThumbnailUrl(String thumbnailUrl) {
    this.thumbnailUrl = thumbnailUrl;
  }

  public String getBannerUrl() {
    return bannerUrl;
  }

  public void setBannerUrl(String bannerUrl) {
    this.bannerUrl = bannerUrl;
  }

  public String getTargetAudience() {
    return targetAudience;
  }

  public void setTargetAudience(String targetAudience) {
    this.targetAudience = targetAudience;
  }

  public String getPrerequisites() {
    return prerequisites;
  }

  public void setPrerequisites(String prerequisites) {
    this.prerequisites = prerequisites;
  }

  public String getLearningOutcomes() {
    return learningOutcomes;
  }

  public void setLearningOutcomes(String learningOutcomes) {
    this.learningOutcomes = learningOutcomes;
  }

  public String getSyllabusSummary() {
    return syllabusSummary;
  }

  public void setSyllabusSummary(String syllabusSummary) {
    this.syllabusSummary = syllabusSummary;
  }

  public String getCertificateInfo() {
    return certificateInfo;
  }

  public void setCertificateInfo(String certificateInfo) {
    this.certificateInfo = certificateInfo;
  }

  public BigDecimal getTuitionFee() {
    return tuitionFee;
  }

  public void setTuitionFee(BigDecimal tuitionFee) {
    this.tuitionFee = tuitionFee;
  }

  public int getTotalSessions() {
    return totalSessions;
  }

  public void setTotalSessions(int totalSessions) {
    this.totalSessions = totalSessions;
  }

  public Integer getDurationHours() {
    return durationHours;
  }

  public void setDurationHours(Integer durationHours) {
    this.durationHours = durationHours;
  }

  public Integer getLevelId() {
    return levelId;
  }

  public void setLevelId(Integer levelId) {
    this.levelId = levelId;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getPublicationStatus() {
    return publicationStatus;
  }

  public void setPublicationStatus(String publicationStatus) {
    this.publicationStatus = publicationStatus;
  }

  public boolean isFeatured() {
    return featured;
  }

  public void setFeatured(boolean featured) {
    this.featured = featured;
  }
}
