package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Date;

public class CourseClassRequest {
  private Integer id;

  @NotBlank
  @Size(max = 30)
  private String classCode;

  @NotBlank
  @Size(max = 200)
  private String className;

  @NotNull private Date startDate;

  @NotNull private Date endDate;

  @Min(1)
  private int maxStudents;

  @NotNull
  @DecimalMin("0.0")
  private BigDecimal appliedTuitionFee;

  @NotNull private Integer courseId;

  private Integer teacherId;

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getClassCode() {
    return classCode;
  }

  public void setClassCode(String classCode) {
    this.classCode = classCode;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public Date getStartDate() {
    return startDate;
  }

  public void setStartDate(Date startDate) {
    this.startDate = startDate;
  }

  public Date getEndDate() {
    return endDate;
  }

  public void setEndDate(Date endDate) {
    this.endDate = endDate;
  }

  public int getMaxStudents() {
    return maxStudents;
  }

  public void setMaxStudents(int maxStudents) {
    this.maxStudents = maxStudents;
  }

  public BigDecimal getAppliedTuitionFee() {
    return appliedTuitionFee;
  }

  public void setAppliedTuitionFee(BigDecimal appliedTuitionFee) {
    this.appliedTuitionFee = appliedTuitionFee;
  }

  public Integer getCourseId() {
    return courseId;
  }

  public void setCourseId(Integer courseId) {
    this.courseId = courseId;
  }

  public Integer getTeacherId() {
    return teacherId;
  }

  public void setTeacherId(Integer teacherId) {
    this.teacherId = teacherId;
  }
}
