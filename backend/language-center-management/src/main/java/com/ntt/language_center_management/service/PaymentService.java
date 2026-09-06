package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.CreatePaymentRequest;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public interface PaymentService {
  PaymentResponse createPayment(CreatePaymentRequest request, Principal principal);
  List<PaymentResponse> getMyPayments(Principal principal);
  Map<String, Object> handleMomoIpn(Map<String, Object> payload);
  Map<String, Object> handleZaloPayCallback(Map<String, Object> payload);
}
