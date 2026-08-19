package com.ntt.language_center_management.enums;

public enum PaymentStatus {
    PENDING,        // Chưa thanh toán hoặc đang chờ thanh toán
    PAID,           // Thanh toán thành công
    FAILED,         // Thanh toán thất bại
    CANCELLED,      // Giao dịch đã bị hủy
}
