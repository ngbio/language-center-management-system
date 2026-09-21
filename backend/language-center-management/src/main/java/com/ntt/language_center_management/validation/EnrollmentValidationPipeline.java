package com.ntt.language_center_management.validation;

import java.util.List;

/** Ordered, fail-fast rules. The pipeline never writes data or starts transactions. */
public final class EnrollmentValidationPipeline {
  private final List<EnrollmentRule> rules;

  public EnrollmentValidationPipeline(List<EnrollmentRule> rules) {
    this.rules = List.copyOf(rules);
  }

  public void validate(EnrollmentValidationContext context) {
    for (EnrollmentRule rule : rules) rule.validate(context);
  }
}
