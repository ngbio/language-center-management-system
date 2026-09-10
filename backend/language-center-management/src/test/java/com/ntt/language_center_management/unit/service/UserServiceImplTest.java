package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.ChangePasswordRequest;
import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.mapper.UserMapper;
import com.ntt.language_center_management.repository.RoleRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.impl.UserServiceImpl;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserServiceImpl userService;
  private final Principal principal = () -> "student@example.com";

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    userService =
        new UserServiceImpl(
            userRepository,
            mock(StudentRepository.class),
            mock(TeacherRepository.class),
            mock(RoleRepository.class),
            mock(UserMapper.class),
            passwordEncoder);
  }

  @Test
  void shouldEncodeAndSaveNewPasswordWhenChangeIsValid() {
    User user = userWithPassword("old-hash");
    ChangePasswordRequest request =
        new ChangePasswordRequest("Old@1234", "New@1234", "New@1234");
    when(userRepository.findByEmailIgnoreCase(principal.getName())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Old@1234", "old-hash")).thenReturn(true);
    when(passwordEncoder.matches("New@1234", "old-hash")).thenReturn(false);
    when(passwordEncoder.encode("New@1234")).thenReturn("new-hash");

    userService.changePassword(principal, request);

    assertEquals("new-hash", user.getPasswordHash());
    verify(passwordEncoder).encode("New@1234");
    verify(userRepository).save(user);
  }

  @Test
  void shouldRejectPasswordChangeWhenCurrentPasswordIsIncorrect() {
    User user = userWithPassword("old-hash");
    ChangePasswordRequest request =
        new ChangePasswordRequest("Wrong@123", "New@1234", "New@1234");
    when(userRepository.findByEmailIgnoreCase(principal.getName())).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("Wrong@123", "old-hash")).thenReturn(false);

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> userService.changePassword(principal, request));

    assertEquals("Mật khẩu hiện tại không chính xác", exception.getMessage());
    verify(passwordEncoder, never()).encode("New@1234");
    verify(userRepository, never()).save(user);
  }

  @Test
  void shouldReturnUnauthorizedWhenLoginEmailDoesNotExist() {
    when(userRepository.findByEmailIgnoreCase("missing@example.com"))
        .thenReturn(Optional.empty());

    assertThrows(
        UnauthorizedException.class,
        () -> userService.login(new LoginRequest("missing@example.com", "secret")));
  }

  private User userWithPassword(String passwordHash) {
    User user = new User();
    user.setEmail(principal.getName());
    user.setPasswordHash(passwordHash);
    return user;
  }
}
