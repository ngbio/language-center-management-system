package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.ClassStatus;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

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
    ClassStatus status,
    Integer courseId,
    String courseCode,
    String courseName,
    Integer levelId,
    String levelCode,
    Integer teacherId,
    String teacherCode,
    String teacherName,
    Date createdAt,
    Date updatedAt,
    List<ClassScheduleResponse> schedules) {}
