package com.ntt.language_center_management.enums;

public enum EnrollmentStatus {
  PENDING,    // Yêu cầu đăng ký đang chờ xử lý; dữ liệu cũ/API vẫn hỗ trợ trạng thái này.
  CONFIRMED,  // Đã giữ chỗ; với lớp có phí vẫn phải thanh toán đúng hạn để có quyền học.
  CANCELLED   // Đăng ký đã hủy, hết hạn thanh toán hoặc đã được hoàn toàn bộ tiền.
}
