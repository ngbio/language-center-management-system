package com.ntt.language_center_management.enums;

public enum ClassStatus {
  DRAFT, // Lớp mới tạo, chưa mở đăng ký
  OPEN, // Đang mở đăng ký
  FULL, // Đã đủ số lượng học viên
  IN_PROGRESS, // Đang diễn ra
  COMPLETED, // Đã kết thúc
  CANCELLED // Đã hủy
}
