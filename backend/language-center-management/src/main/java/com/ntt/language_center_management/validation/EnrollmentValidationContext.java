package com.ntt.language_center_management.validation;

import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Student;

/** Classes must already be locked by the calling transaction. */
public record EnrollmentValidationContext(Student student, Courseclass targetClass, Courseclass sourceClass, Integer sourceEnrollmentId) {
  public static EnrollmentValidationContext registration(Student student, Courseclass targetClass) {
    return new EnrollmentValidationContext(student, targetClass, null, null);
  }

  public static EnrollmentValidationContext transfer(Student student, Courseclass sourceClass, Courseclass targetClass, Integer sourceEnrollmentId) {
    return new EnrollmentValidationContext(student, targetClass, sourceClass, java.util.Objects.requireNonNull(sourceEnrollmentId));
  }
}
