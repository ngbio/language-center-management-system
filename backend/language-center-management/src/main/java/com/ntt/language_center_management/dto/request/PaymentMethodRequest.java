package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record PaymentMethodRequest(
    @NotBlank(message = "Phương thức thanh toán không được để trống")
        @Pattern(regexp = "MOMO|ZALOPAY", message = "Phương thức phải là MOMO hoặc ZALOPAY")
        String method) {}
