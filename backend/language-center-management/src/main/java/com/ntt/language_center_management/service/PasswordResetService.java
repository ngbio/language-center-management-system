package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.ResetPasswordRequest;

public interface PasswordResetService {
  void requestReset(String email);
  boolean isTokenValid(String rawToken);
  void resetPassword(ResetPasswordRequest request);
}
