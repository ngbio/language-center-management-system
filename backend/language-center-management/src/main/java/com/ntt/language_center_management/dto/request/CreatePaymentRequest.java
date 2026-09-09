package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
    @NotNull(message = "Đăng ký không được để trống") Integer enrollmentId,
    @NotNull(message = "Phương thức thanh toán không được để trống") PaymentMethod method) {}
