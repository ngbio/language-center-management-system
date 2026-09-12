package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.management.BillingApiController;
import com.ntt.language_center_management.controller.student.PaymentApiController;
import com.ntt.language_center_management.dto.request.CreatePaymentRequest;
import com.ntt.language_center_management.dto.request.RefundRequest;
import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.InvoicePdfService;
import com.ntt.language_center_management.service.PaymentService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PaymentApiController.class, BillingApiController.class})
@Import(SecurityConfig.class)
class PaymentRefundControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private PaymentService paymentService;
  @MockitoBean private BillingService billingService;
  @MockitoBean private InvoicePdfService invoicePdfService;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentCreatesPaymentForOwnEnrollment() throws Exception {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), any(Principal.class)))
        .thenReturn(payment());

    mockMvc.perform(post("/api/enrollments/15/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"MOMO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.enrollmentId").value(15))
        .andExpect(jsonPath("$.data.method").value("MOMO"));

    verify(paymentService).createPayment(
        eq(new CreatePaymentRequest(15, com.ntt.language_center_management.enums.PaymentMethod.MOMO)),
        argThat(principal -> principal.getName().equals("student@example.com")));
  }

  @Test
  @WithMockUser(username = "other@example.com", roles = "STUDENT")
  void studentCannotCreatePaymentForAnotherStudentsEnrollment() throws Exception {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), any(Principal.class)))
        .thenThrow(new ForbiddenException("Không có quyền thanh toán enrollment này"));

    mockMvc.perform(post("/api/enrollments/15/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"MOMO\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void legacyPaymentEndpointStillRequiresStudentAndDelegatesPrincipal() throws Exception {
    when(paymentService.createPayment(any(CreatePaymentRequest.class), any(Principal.class)))
        .thenReturn(payment());

    mockMvc.perform(post("/api/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"enrollmentId\":15,\"method\":\"MOMO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.enrollmentId").value(15));
  }

  @Test
  @WithMockUser(roles = "TEACHER")
  void teacherCannotCreatePayment() throws Exception {
    mockMvc.perform(post("/api/enrollments/15/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"MOMO\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void paymentRequiresAuthentication() throws Exception {
    mockMvc.perform(post("/api/enrollments/15/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"method\":\"MOMO\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void paymentRejectsMissingMethod() throws Exception {
    mockMvc.perform(post("/api/enrollments/15/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentReadsOwnPaymentHistory() throws Exception {
    when(paymentService.getMyPayments(any(Principal.class))).thenReturn(List.of(payment()));
    when(billingService.getPayments(eq(15), any(Principal.class))).thenReturn(List.of(payment()));

    mockMvc.perform(get("/api/students/me/payments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].transactionCode").value("TXN-15"));
    mockMvc.perform(get("/api/enrollments/15/payments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].status").value("PENDING"));
  }

  @Test
  @WithMockUser(username = "other@example.com", roles = "STUDENT")
  void paymentHistoryRejectsAnotherEnrollmentOwner() throws Exception {
    when(billingService.getPayments(eq(15), any(Principal.class)))
        .thenThrow(new ForbiddenException("Không có quyền xem giao dịch"));

    mockMvc.perform(get("/api/enrollments/15/payments"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentLooksUpOwnedPaymentByTransactionCode() throws Exception {
    when(billingService.getPayment(eq("TXN-15"), any(Principal.class))).thenReturn(payment());

    mockMvc.perform(get("/api/payments/TXN-15"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.transactionCode").value("TXN-15"));

    verify(billingService).getPayment(eq("TXN-15"),
        argThat(principal -> principal.getName().equals("student@example.com")));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentReadsInvoiceForOwnEnrollment() throws Exception {
    when(billingService.getInvoice(eq(15), any(Principal.class))).thenReturn(invoice());

    mockMvc.perform(get("/api/enrollments/15/invoice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.invoiceNumber").value("INV-15"))
        .andExpect(jsonPath("$.data.enrollmentId").value(15))
        .andExpect(jsonPath("$.data.netPaidAmount").value(100000));

    verify(billingService).getInvoice(eq(15),
        argThat(principal -> principal.getName().equals("student@example.com")));
  }

  @Test
  @WithMockUser(username = "consultant@example.com", roles = "CONSULTANT")
  void authorizedStaffReadsStudentInvoice() throws Exception {
    when(billingService.getInvoice(eq(15), any(Principal.class))).thenReturn(invoice());

    mockMvc.perform(get("/api/enrollments/15/invoice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.invoiceNumber").value("INV-15"));
  }

  @Test
  void gatewayCallbacksRemainPublicAndDelegatePayload() throws Exception {
    when(paymentService.handleMomoIpn(any())).thenReturn(Map.of("resultCode", 0));
    when(paymentService.handleZaloPayCallback(any())).thenReturn(Map.of("return_code", 1));

    mockMvc.perform(post("/api/payments/momo/ipn")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"orderId\":\"TXN-15\",\"signature\":\"signed\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resultCode").value(0));
    mockMvc.perform(post("/api/payments/zalopay/callback")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"data\":\"signed-data\",\"mac\":\"signed\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.return_code").value(1));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void invoicePdfHasExpectedContentTypeFilenameAndBody() throws Exception {
    when(invoicePdfService.createInvoicePdf(eq(15), any(Principal.class)))
        .thenReturn("%PDF-test".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    mockMvc.perform(get("/api/enrollments/15/invoice.pdf").param("download", "true"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"invoice-enrollment-15.pdf\""))
        .andExpect(content().bytes("%PDF-test".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }

  @Test
  @WithMockUser(username = "consultant@example.com", roles = "CONSULTANT")
  void consultantCreatesRefundWithIdempotencyKey() throws Exception {
    mockMvc.perform(post("/api/staff/enrollments/15/refunds")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"amount":100000,"reason":"Học viên yêu cầu","idempotencyKey":"RF-15-01"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(201));

    verify(billingService).createRefund(eq(15), any(RefundRequest.class),
        argThat(principal -> principal.getName().equals("consultant@example.com")));
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotUseStaffRefundEndpoints() throws Exception {
    mockMvc.perform(get("/api/staff/refunds"))
        .andExpect(status().isForbidden());
    mockMvc.perform(post("/api/staff/refunds/9/refresh"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "CONSULTANT")
  void refundValidationRejectsEmptyReasonAndKey() throws Exception {
    mockMvc.perform(post("/api/staff/enrollments/15/refunds")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"amount\":0,\"reason\":\"\",\"idempotencyKey\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  private PaymentResponse payment() {
    return new PaymentResponse(3, 15, "TXN-15", "MOMO", new BigDecimal("100000"),
        "PENDING", "https://pay.example/15", new Date(), null);
  }

  private InvoiceResponse invoice() {
    return new InvoiceResponse("INV-15", 15, "ST001", "Student One",
        "student@example.com", "EN-A1", "English A1", "EN-A1-01",
        "English A1 Morning", new BigDecimal("100000"), new BigDecimal("100000"),
        BigDecimal.ZERO, new BigDecimal("100000"), "CONFIRMED", "PAID", new Date(),
        List.of(payment()), List.of());
  }
}
