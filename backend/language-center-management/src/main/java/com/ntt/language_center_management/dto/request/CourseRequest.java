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

  @Size(max = 1000)
  private String description;

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

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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
}
