package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class PasswordChangePolicy {
  private final PasswordEncoder passwordEncoder;
  public PasswordChangePolicy(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  public void validateCurrent(String currentPassword, User user) {
    if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
      throw new IllegalArgumentException("Mật khẩu hiện tại không chính xác");
    }
  }
  public void validateConfirmation(String newPassword, String confirmPassword) {
    if (!newPassword.equals(confirmPassword)) {
      throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp");
    }
  }
  public void validateDifferent(String newPassword, User user) {
    if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
      throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
    }
  }

}
