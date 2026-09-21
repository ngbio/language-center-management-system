package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.invoice.InvoiceExportService;
import com.ntt.language_center_management.invoice.InvoiceRenderer;
import com.ntt.language_center_management.invoice.PdfInvoiceRenderer;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.InvoicePdfService;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoicePdfServiceImpl extends InvoiceExportService implements InvoicePdfService {
  @Value("${invoice.pdf.font-path:}")
  private String fontPath;

  public InvoicePdfServiceImpl(BillingService billingService) { super(billingService); }

  @Override
  protected InvoiceRenderer createRenderer() { return new PdfInvoiceRenderer(fontPath); }

  @Override
  @Transactional(readOnly = true)
  public byte[] createInvoicePdf(Integer enrollmentId, Principal principal) {
    return export(enrollmentId, principal).content();
  }
}
