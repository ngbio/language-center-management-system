package com.ntt.language_center_management.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalTime;

public record ClassScheduleResponse(
    Integer id,
    Integer courseClassId,
    String classCode,
    String className,
    short dayOfWeek,
    @JsonFormat(pattern = "HH:mm") LocalTime startTime,
    @JsonFormat(pattern = "HH:mm") LocalTime endTime,
    String deliveryMode,
    Integer roomId,
    String roomCode,
    String roomName,
    String meetingUrl) {}
