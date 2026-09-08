package com.ntt.language_center_management.dto.response;

import java.util.Date;
import java.util.List;

public record AttendanceSheetResponse(
    Integer lessonId,
    Date lessonDate,
    String lessonTopic,
    String lessonStatus,
    Integer classId,
    String classCode,
    String className,
    List<AttendanceSheetItemResponse> students) {}
