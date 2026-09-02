package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.Size;

public record LessonUpdateRequest(
    @Size(max = 255, message = "Chủ đề không được vượt quá 255 ký tự") String topic,
    @Size(max = 500, message = "Đường dẫn phòng học không được vượt quá 500 ký tự")
        String meetingUrl) {}
