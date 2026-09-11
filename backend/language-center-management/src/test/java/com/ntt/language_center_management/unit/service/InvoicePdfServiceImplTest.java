package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.impl.InvoicePdfServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InvoicePdfServiceImplTest {

  @Test
  void shouldRenderInvoiceContentAsPdfAndCallBillingWithCurrentPrincipal() throws Exception {
    BillingService billingService = mock(BillingService.class);
    Principal principal = () -> "student@example.com";
    InvoiceResponse invoice = new InvoiceResponse(
        "INV-15", 15, "HV000015", "Nguyễn Văn An", "student@example.com",
        "JA-N5", "Tiếng Nhật N5", "JA-N5-01", "Lớp tiếng Nhật buổi tối",
        new BigDecimal("3200000"), new BigDecimal("3200000"), BigDecimal.ZERO,
        new BigDecimal("3200000"), "CONFIRMED", "PAID", new Date(), List.of(), List.of());
    when(billingService.getInvoice(15, principal)).thenReturn(invoice);

    byte[] pdf = new InvoicePdfServiceImpl(billingService).createInvoicePdf(15, principal);

    assertThat(pdf.length).isGreaterThan(1_000);
    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    try (var document = Loader.loadPDF(pdf)) {
      String text = new PDFTextStripper().getText(document);
      assertThat(text).contains("INV-15", "HV000015", "student@example.com", "JA-N5", "JA-N5-01");
    }
    verify(billingService).getInvoice(15, principal);
  }

  @Test
  void shouldSafelyRenderNullAmountsDatesAndHistoryLists() {
    BillingService billingService = mock(BillingService.class);
    Principal principal = () -> "student@example.com";
    InvoiceResponse invoice = new InvoiceResponse(
        "INV-16", 16, "HV000016", "Student", "student@example.com",
        "EN-A1", "English", "EN-A1-01", "Morning class",
        null, null, null, null, "CONFIRMED", "PENDING", null, null, null);
    when(billingService.getInvoice(16, principal)).thenReturn(invoice);

    byte[] pdf = new InvoicePdfServiceImpl(billingService).createInvoicePdf(16, principal);

    assertThat(pdf).isNotEmpty();
  }

  @Test
  void shouldFailClearlyWhenConfiguredUnicodeFontDoesNotExist() {
    BillingService billingService = mock(BillingService.class);
    InvoicePdfServiceImpl service = new InvoicePdfServiceImpl(billingService);
    ReflectionTestUtils.setField(service, "fontPath", "missing/font/not-found.ttf");
    when(billingService.getInvoice(org.mockito.ArgumentMatchers.eq(17),
        org.mockito.ArgumentMatchers.any())).thenReturn(new InvoiceResponse(
            "INV-17", 17, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, List.of(), List.of()));

    org.assertj.core.api.Assertions.assertThatThrownBy(
        () -> service.createInvoicePdf(17, () -> "student@example.com"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("font Unicode");
  }
}
