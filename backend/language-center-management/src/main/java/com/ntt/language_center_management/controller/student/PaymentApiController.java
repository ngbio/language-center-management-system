package com.ntt.language_center_management.controller.student;

import com.ntt.language_center_management.dto.request.CreatePaymentRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.service.PaymentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PaymentApiController {
  private final PaymentService paymentService;

  public PaymentApiController(PaymentService paymentService) {
    this.paymentService = paymentService;
  }

  @PostMapping("/payments")
  public ApiResponse<PaymentResponse> create(
      @Valid @RequestBody CreatePaymentRequest request, Principal principal) {
    return new ApiResponse<>(200, "Tạo yêu cầu thanh toán thành công", paymentService.createPayment(request, principal));
  }

  @GetMapping("/students/me/payments")
  public ApiResponse<List<PaymentResponse>> getMine(Principal principal) {
    return new ApiResponse<>(200, "Lấy lịch sử thanh toán thành công", paymentService.getMyPayments(principal));
  }

  @PostMapping("/payments/momo/ipn")
  public Map<String, Object> momoIpn(@RequestBody Map<String, Object> payload) {
    return paymentService.handleMomoIpn(payload);
  }

  @PostMapping("/payments/zalopay/callback")
  public Map<String, Object> zaloPayCallback(@RequestBody Map<String, Object> payload) {
    return paymentService.handleZaloPayCallback(payload);
  }
}
