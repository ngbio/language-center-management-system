package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.repository.UserRepository;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class AccountUniquenessValidator {
  private final UserRepository userRepository;
  public AccountUniquenessValidator(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public void validate(String normalizedEmail, String normalizedUsername) {
    if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
      throw new DuplicateResourceException("Email này đã có người đăng ký!");
    }
    if (userRepository.existsByUsernameIgnoreCase(normalizedUsername)) {
      throw new DuplicateResourceException("Tên đăng nhập này đã tồn tại!");
    }

  }

}
