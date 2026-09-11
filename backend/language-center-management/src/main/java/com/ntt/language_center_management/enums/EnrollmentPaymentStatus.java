package com.ntt.language_center_management.enums;

public enum EnrollmentPaymentStatus {
  PENDING,    // Enrollment đang chờ học viên thanh toán.
  PAID,       // Enrollment đã có giao dịch thanh toán thành công.
  FAILED,     // Trạng thái thanh toán tổng hợp thất bại; hiện chưa có luồng gán trực tiếp.
  CANCELLED,  // Nghĩa vụ thanh toán bị hủy do enrollment bị hủy hoặc hết hạn.
  REFUNDED    // Toàn bộ tiền đã thu của enrollment đã được hoàn thành công.
}
