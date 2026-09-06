package com.ntt.language_center_management.service;

public interface EnrollmentExpirationService {
  boolean expireIfOverdue(Integer enrollmentId);
  void expireOverdueEnrollments();
}
