package com.ntt.language_center_management.enums;

public enum PaymentTransactionStatus {
  PENDING,   // Giao dịch đã tạo nhưng cổng chưa xác nhận kết quả cuối cùng.
  PAID,      // Cổng thanh toán đã xác nhận thu tiền thành công.
  FAILED,    // Cổng từ chối hoặc xác nhận giao dịch thất bại.
  CANCELLED  // Giao dịch bị hủy; hiện chưa có luồng gán trực tiếp trong service.
}
