package com.ntt.language_center_management.enums;

public enum ClassStatus {
  DRAFT,       // Lớp mới tạo, còn được cấu hình và chưa mở đăng ký.
  OPEN,        // Lớp đang mở để Student hoặc Staff đăng ký học viên.
  FULL,        // Lớp đã đủ số lượng học viên, không nhận thêm đăng ký.
  IN_PROGRESS, // Lớp đã khai giảng và đang trong thời gian học.
  COMPLETED,   // Lớp đã kết thúc chương trình.
  CANCELLED    // Lớp bị hủy, không tiếp tục đăng ký, học hoặc điểm danh.
}
