package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.enums.EnrollmentStatus;
import java.util.Set;

public final class EnrollmentPolicy {

  public static final Set<EnrollmentStatus> CAPACITY_RESERVED_STATUSES =
      Set.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

  private EnrollmentPolicy() {}
}
