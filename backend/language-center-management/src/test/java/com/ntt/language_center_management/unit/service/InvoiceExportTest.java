package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.invoice.*;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.impl.InvoiceHtmlService;
import com.ntt.language_center_management.service.impl.InvoicePdfServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class InvoiceExportTest {
  private final Principal principal = () -> "student@example.com";

  @Test
  void htmlKeepsVietnameseAndEscapesUntrustedInvoiceValues() {
    var billing = mock(BillingService.class);
    when(billing.getInvoice(15, principal)).thenReturn(invoice("Nguyễn An <script>alert(1)</script>", List.of()));
    var result = new InvoiceHtmlService(billing, "Asia/Ho_Chi_Minh").export(15, principal);
    String html = new String(result.content(), StandardCharsets.UTF_8);
    assertThat(result.filename()).isEqualTo("invoice-enrollment-15.html");
    assertThat(result.contentType()).isEqualTo("text/html;charset=UTF-8");
    assertThat(html).contains("Nguyễn An &lt;script&gt;alert(1)&lt;/script&gt;", "HÓA ĐƠN HỌC PHÍ", "Chưa có giao dịch.")
        .doesNotContain("<script>");
    verify(billing).getInvoice(15, principal);
  }

  @Test
  void authorizationFailureStopsBeforeFactoryMethodCreatesRenderer() {
    var billing = mock(BillingService.class);
    when(billing.getInvoice(15, principal)).thenThrow(new UnauthorizedException("Không có quyền"));
    var exporter = new InvoiceExportService(billing) {
      @Override protected InvoiceRenderer createRenderer() {
        throw new AssertionError("Renderer must not be created before authorization");
      }
    };
    assertThatThrownBy(() -> exporter.export(15, principal)).isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void pdfFactoryPreservesVietnameseAndPaginationForLongHistory() throws Exception {
    var billing = mock(BillingService.class);
    var payments = IntStream.range(0, 100).mapToObj(index -> new PaymentResponse(index, 15,
        "TX-" + index, "MOMO", BigDecimal.TEN, "PAID", null, new Date(), new Date())).toList();
    when(billing.getInvoice(15, principal)).thenReturn(invoice("Nguyễn Văn An", payments));
    var result = new InvoicePdfServiceImpl(billing).export(15, principal);
    assertThat(result.contentType()).isEqualTo("application/pdf");
    try (var pdf = Loader.loadPDF(result.content())) {
      assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
      assertThat(new PDFTextStripper().getText(pdf)).contains("Nguyễn Văn An", "TX-0", "TX-99");
    }
  }

  private InvoiceResponse invoice(String studentName, List<PaymentResponse> payments) {
    return new InvoiceResponse("INV-15", 15, "HV15", studentName, "student@example.com", "EN-A1",
        "Tiếng Anh", "EN-A1-01", "Lớp tối", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO,
        BigDecimal.TEN, "CONFIRMED", "PAID", new Date(), payments, List.of());
  }
}
