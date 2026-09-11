package com.ntt.language_center_management.unit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.controller.management.BillingApiController;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.InvoicePdfService;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BillingApiControllerTest {
  private InvoicePdfService invoicePdfService;
  private BillingApiController controller;
  private Principal principal;

  @BeforeEach
  void setUp() {
    invoicePdfService = mock(InvoicePdfService.class);
    controller = new BillingApiController(mock(BillingService.class), invoicePdfService);
    principal = () -> "student@example.com";
    when(invoicePdfService.createInvoicePdf(15, principal)).thenReturn("%PDF".getBytes());
  }

  @Test
  void shouldReturnInlinePdfForPreviewByDefault() {
    var response = controller.invoicePdf(15, false, principal);

    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/pdf");
    assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
    assertThat(response.getHeaders().getContentDisposition().getFilename())
        .isEqualTo("invoice-enrollment-15.pdf");
    assertThat(response.getBody()).startsWith((byte) '%', (byte) 'P', (byte) 'D', (byte) 'F');
  }

  @Test
  void shouldReturnAttachmentPdfWhenDownloadIsRequested() {
    var response = controller.invoicePdf(15, true, principal);

    assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("attachment");
  }
}
