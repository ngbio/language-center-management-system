package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.RefundRequest;
import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.dto.response.RefundResponse;
import java.security.Principal;
import java.util.List;

public interface BillingService {
  List<PaymentResponse> getPayments(Integer enrollmentId, Principal principal);
  PaymentResponse getPayment(String transactionCode, Principal principal);
  List<RefundResponse> getRefunds(Integer enrollmentId, Principal principal);
  RefundResponse createRefund(Integer enrollmentId, RefundRequest request, Principal principal);
  InvoiceResponse getInvoice(Integer enrollmentId, Principal principal);
}
