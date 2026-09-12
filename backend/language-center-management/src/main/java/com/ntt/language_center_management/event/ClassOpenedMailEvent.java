package com.ntt.language_center_management.event;

import java.math.BigDecimal;
import java.util.Date;

public record ClassOpenedMailEvent(
    String classCode,
    String className,
    Date startDate,
    BigDecimal tuitionFee) {}
