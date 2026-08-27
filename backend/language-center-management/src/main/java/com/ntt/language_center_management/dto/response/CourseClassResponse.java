package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.util.Date;

public record CourseClassResponse(
    Integer id,
    String classCode,
    String className,
    Date startDate,
    Date endDate,
    int maxStudents,
    long enrolledStudents,
    long availableSeats,
    BigDecimal appliedTuitionFee,
    String status,
    Integer courseId,
    String courseCode,
    String courseName,
    Integer levelId,
    String levelCode,
    Integer teacherId,
    String teacherCode,
    String teacherName,
    Date createdAt,
    Date updatedAt) {}
