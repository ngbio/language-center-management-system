package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.time.YearMonth;

public record RevenueReportResponse(
    YearMonth month,
    BigDecimal grossRevenue,
    BigDecimal refundedAmount,
    BigDecimal netRevenue) {}
