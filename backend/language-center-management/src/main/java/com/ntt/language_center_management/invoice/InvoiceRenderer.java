package com.ntt.language_center_management.invoice;

import com.ntt.language_center_management.dto.response.InvoiceResponse;

public interface InvoiceRenderer {
  ExportedDocument render(InvoiceResponse invoice);
}
