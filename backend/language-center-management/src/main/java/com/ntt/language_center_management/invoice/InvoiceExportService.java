package com.ntt.language_center_management.invoice;

import com.ntt.language_center_management.service.BillingService;
import java.security.Principal;
import org.springframework.transaction.annotation.Transactional;

/** Creator: the shared export workflow obtains authorized data before creating a renderer. */
public abstract class InvoiceExportService {
  private final BillingService billingService;

  protected InvoiceExportService(BillingService billingService) { this.billingService = billingService; }

  @Transactional(readOnly = true)
  public ExportedDocument export(Integer enrollmentId, Principal principal) {
    var invoice = billingService.getInvoice(enrollmentId, principal);
    return createRenderer().render(invoice);
  }

  protected abstract InvoiceRenderer createRenderer();
}
