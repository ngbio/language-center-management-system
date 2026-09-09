package com.ntt.language_center_management.dto.request;

import com.ntt.language_center_management.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentMethodRequest(
    @NotNull(message = "Phương thức thanh toán không được để trống") PaymentMethod method) {}
