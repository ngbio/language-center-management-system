package com.ntt.language_center_management.dto.response;

import java.time.YearMonth;

public record EnrollmentReportResponse(
    YearMonth month,
    long total,
    long confirmed,
    long paid,
    long cancelled) {}
