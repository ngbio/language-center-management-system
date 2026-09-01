package com.ntt.language_center_management.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalTime;

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
    String status,
    String deliveryMode,
    Integer roomId,
    String roomCode,
    String roomName,
    String meetingUrl) {}
