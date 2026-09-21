package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.PaymentMethod;
import java.util.Map;
import java.util.function.Consumer;

public interface PaymentGateway {
  PaymentMethod method();
  PaymentCheckout createPayment(PaymentCommand command);
  Map<String, Object> handleCallback(Map<String, Object> payload, Consumer<PaymentCallback> completion);
}
