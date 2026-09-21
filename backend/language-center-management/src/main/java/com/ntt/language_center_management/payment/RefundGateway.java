package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.PaymentMethod;

public interface RefundGateway {
  PaymentMethod method();
  void validate(String referenceCode);
  String createRefundCode(Integer enrollmentId);
  RefundResult submit(RefundCommand command);
  RefundResult query(RefundCommand command);
}
