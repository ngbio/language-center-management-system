package com.ntt.language_center_management.enums;

public enum RefundStatus {
  PENDING,   // Đã gửi hoặc đang chờ đối soát kết quả từ cổng thanh toán.
  COMPLETED, // Cổng xác nhận hoàn tiền thành công.
  FAILED,    // Cổng xác nhận yêu cầu hoàn tiền thất bại.
  CANCELLED  // Yêu cầu hoàn bị hủy; hiện chưa có API chuyển sang trạng thái này.
}
