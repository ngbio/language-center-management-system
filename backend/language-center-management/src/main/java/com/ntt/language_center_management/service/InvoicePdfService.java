package com.ntt.language_center_management.service;

import java.security.Principal;

public interface InvoicePdfService {
  byte[] createInvoicePdf(Integer enrollmentId, Principal principal);
}
