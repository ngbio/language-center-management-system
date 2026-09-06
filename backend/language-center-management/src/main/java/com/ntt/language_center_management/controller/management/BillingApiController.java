package com.ntt.language_center_management.controller.management;

import com.ntt.language_center_management.dto.request.RefundRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.dto.response.RefundResponse;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.InvoicePdfService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api")
public class BillingApiController {
  private final BillingService billingService;
  private final InvoicePdfService invoicePdfService;

  public BillingApiController(BillingService billingService, InvoicePdfService invoicePdfService) {
    this.billingService = billingService;
    this.invoicePdfService = invoicePdfService;
  }

  @GetMapping("/enrollments/{id}/payments")
  public ApiResponse<List<PaymentResponse>> payments(@PathVariable Integer id, Principal principal) {
    return new ApiResponse<>(200, "Lấy lịch sử thanh toán thành công", billingService.getPayments(id, principal));
  }

  @GetMapping("/payments/{transactionCode}")
  public ApiResponse<PaymentResponse> payment(@PathVariable String transactionCode, Principal principal) {
    return new ApiResponse<>(200, "Lấy trạng thái giao dịch thành công", billingService.getPayment(transactionCode, principal));
  }

  @GetMapping("/enrollments/{id}/refunds")
  public ApiResponse<List<RefundResponse>> refunds(@PathVariable Integer id, Principal principal) {
    return new ApiResponse<>(200, "Lấy lịch sử hoàn tiền thành công", billingService.getRefunds(id, principal));
  }

  @PostMapping("/staff/enrollments/{id}/refunds")
  public ApiResponse<RefundResponse> refund(@PathVariable Integer id,
      @Valid @RequestBody RefundRequest request, Principal principal) {
    return new ApiResponse<>(201, "Hoàn tiền thành công", billingService.createRefund(id, request, principal));
  }

  @GetMapping("/enrollments/{id}/invoice")
  public ApiResponse<InvoiceResponse> invoice(@PathVariable Integer id, Principal principal) {
    return new ApiResponse<>(200, "Lấy hóa đơn thành công", billingService.getInvoice(id, principal));
  }

  @GetMapping(value = "/enrollments/{id}/invoice.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> invoicePdf(@PathVariable Integer id, Principal principal) {
    byte[] pdf = invoicePdfService.createInvoicePdf(id, principal);
    String filename = "invoice-enrollment-" + id + ".pdf";
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.length)
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(filename).build().toString())
        .body(pdf);
  }
}
