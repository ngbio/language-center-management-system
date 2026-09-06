package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record RefundRequest(
    @DecimalMin(value = "0.01", message = "Số tiền hoàn phải lớn hơn 0") BigDecimal amount,
    @NotBlank(message = "Lý do hoàn tiền không được để trống")
        @Size(max = 500, message = "Lý do hoàn tiền tối đa 500 ký tự") String reason,
    @NotBlank(message = "Idempotency key không được để trống")
        @Size(max = 100, message = "Idempotency key tối đa 100 ký tự") String idempotencyKey) {}
