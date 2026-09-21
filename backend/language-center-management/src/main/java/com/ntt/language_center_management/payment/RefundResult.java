package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.RefundStatus;

public record RefundResult(RefundStatus status, String gatewayRefundId, String message) {}
