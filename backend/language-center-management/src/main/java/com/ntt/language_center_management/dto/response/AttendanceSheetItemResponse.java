package com.ntt.language_center_management.dto.response;

import java.util.Date;

public record AttendanceSheetItemResponse(
    Integer attendanceId,
    Integer studentId,
    String studentCode,
    String studentName,
    String status,
    String note,
    Date attendanceTime) {}
