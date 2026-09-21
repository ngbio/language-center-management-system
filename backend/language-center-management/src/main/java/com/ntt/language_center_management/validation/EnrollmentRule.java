package com.ntt.language_center_management.validation;

@FunctionalInterface
public interface EnrollmentRule {
  void validate(EnrollmentValidationContext context);
}
