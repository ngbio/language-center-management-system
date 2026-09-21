package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.invoice.HtmlInvoiceRenderer;
import com.ntt.language_center_management.invoice.InvoiceExportService;
import com.ntt.language_center_management.invoice.InvoiceRenderer;
import com.ntt.language_center_management.service.BillingService;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class InvoiceHtmlService extends InvoiceExportService {
  private final ZoneId zone;

  public InvoiceHtmlService(BillingService billingService,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String timeZone) {
    super(billingService);
    this.zone = ZoneId.of(timeZone);
  }

  @Override
  protected InvoiceRenderer createRenderer() { return new HtmlInvoiceRenderer(zone); }
}
