package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.DeliveryMode;
import com.ntt.language_center_management.enums.LessonStatus;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

public record LessonResponse(
    Integer id,
    Integer courseClassId,
    String classCode,
    String className,
    Integer classScheduleId,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate lessonDate,
    @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm") LocalTime endTime,
    String topic,
    LessonStatus status,
    DeliveryMode deliveryMode,
    Integer roomId,
    String roomCode,
    String roomName,
    String meetingUrl,
    @JsonFormat(pattern = "yyyy-MM-dd") LocalDate originalLessonDate,
    String rescheduleReason,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss") LocalDateTime rescheduledAt,
    String rescheduledBy) {}
