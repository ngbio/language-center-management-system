package com.ntt.language_center_management.enums;

public enum LessonStatus {
  SCHEDULED,   // Buổi học đã được sinh và chưa qua giờ kết thúc.
  IN_PROGRESS, // Buổi học đang diễn ra; hiện chưa có thao tác chuyển trạng thái trực tiếp.
  COMPLETED,   // Buổi học đã kết thúc; scheduler tự động cập nhật theo lịch.
  CANCELLED    // Buổi học đã bị hủy và không được tính như buổi học hợp lệ.
}
