package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
    @NotBlank @Size(max = 16000) String idToken,
    @Size(max = 100) String password) {
  @Override
  public String toString() {
    return "GoogleLoginRequest[credentials=REDACTED]";
  }
}
