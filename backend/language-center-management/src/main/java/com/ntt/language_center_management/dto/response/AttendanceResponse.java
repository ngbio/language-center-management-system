package com.ntt.language_center_management.dto.response;

import java.util.Date;

public record AttendanceResponse(
    Integer id,
    Integer lessonId,
    Date lessonDate,
    String lessonTopic,
    Integer classId,
    String classCode,
    String className,
    Integer studentId,
    String studentCode,
    String studentName,
    String status,
    String note,
    Date attendanceTime) {}
