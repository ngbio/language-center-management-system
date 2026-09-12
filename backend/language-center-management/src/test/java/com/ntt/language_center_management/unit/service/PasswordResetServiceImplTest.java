package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.ResetPasswordRequest;
import com.ntt.language_center_management.entity.PasswordResetToken;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.event.PasswordResetCompletedMailEvent;
import com.ntt.language_center_management.event.PasswordResetRequestedMailEvent;
import com.ntt.language_center_management.repository.PasswordResetTokenRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.impl.PasswordResetServiceImpl;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordResetServiceImplTest {
  private UserRepository users;
  private PasswordResetTokenRepository tokens;
  private PasswordEncoder passwordEncoder;
  private ApplicationEventPublisher events;
  private PasswordResetServiceImpl service;

  @BeforeEach
  void setUp() {
    users = org.mockito.Mockito.mock(UserRepository.class);
    tokens = org.mockito.Mockito.mock(PasswordResetTokenRepository.class);
    passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
    events = org.mockito.Mockito.mock(ApplicationEventPublisher.class);
    service = new PasswordResetServiceImpl(
        users, tokens, passwordEncoder, events, "https://app.example.com/", 30, 60);
  }

  @Test
  void requestResetStoresOnlyHashedTokenAndPublishesFrontendLink() {
    User user = activeUser();
    when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(user));

    service.requestReset(" Student@Example.com ");

    ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
    verify(tokens).invalidateActiveTokens(eq(7), any(Date.class));
    verify(tokens).save(tokenCaptor.capture());
    PasswordResetToken stored = tokenCaptor.getValue();
    assertThat(stored.getTokenHash()).matches("[0-9a-f]{64}");
    assertThat(stored.getExpiresAt()).isAfter(stored.getCreatedAt());

    ArgumentCaptor<PasswordResetRequestedMailEvent> eventCaptor =
        ArgumentCaptor.forClass(PasswordResetRequestedMailEvent.class);
    verify(events).publishEvent(eventCaptor.capture());
    PasswordResetRequestedMailEvent event = eventCaptor.getValue();
    assertThat(event.resetUrl()).startsWith("https://app.example.com/reset-password?token=");
    assertThat(event.resetUrl()).doesNotContain(stored.getTokenHash());
    assertThat(event.expirationMinutes()).isEqualTo(30);
  }

  @Test
  void requestResetDoesNotRevealOrSendForUnknownInactiveOrRateLimitedAccount() {
    service.requestReset("missing@example.com");
    verify(events, never()).publishEvent(any());

    User inactive = activeUser();
    inactive.setStatus(AccountStatus.INACTIVE);
    when(users.findByEmailIgnoreCase("inactive@example.com")).thenReturn(Optional.of(inactive));
    service.requestReset("inactive@example.com");

    User active = activeUser();
    when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(active));
    when(tokens.existsByUserId_IdAndCreatedAtAfterAndUsedAtIsNull(eq(7), any(Date.class)))
        .thenReturn(true);
    service.requestReset("student@example.com");

    verify(tokens, never()).save(any());
    verify(events, never()).publishEvent(any());
  }

  @Test
  void validatesOnlyUnusedAndUnexpiredTokens() {
    PasswordResetToken valid = resetToken(activeUser(), new Date(System.currentTimeMillis() + 60_000));
    when(tokens.findByTokenHash(any())).thenReturn(Optional.of(valid));
    assertThat(service.isTokenValid("raw-token")).isTrue();

    valid.setUsedAt(new Date());
    assertThat(service.isTokenValid("raw-token")).isFalse();

    valid.setUsedAt(null);
    valid.setExpiresAt(new Date(System.currentTimeMillis() - 1_000));
    assertThat(service.isTokenValid("raw-token")).isFalse();
    assertThat(service.isTokenValid(" ")).isFalse();
  }

  @Test
  void resetPasswordChangesHashConsumesAllTokensAndPublishesConfirmation() {
    User user = activeUser();
    user.setPasswordHash("old-hash");
    PasswordResetToken token = resetToken(user, new Date(System.currentTimeMillis() + 60_000));
    when(tokens.lockByTokenHash(any())).thenReturn(Optional.of(token));
    when(passwordEncoder.matches("New@1234", "old-hash")).thenReturn(false);
    when(passwordEncoder.encode("New@1234")).thenReturn("new-hash");

    service.resetPassword(new ResetPasswordRequest("raw-token", "New@1234", "New@1234"));

    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    assertThat(user.getPasswordChangedAt()).isNotNull();
    assertThat(token.getUsedAt()).isNotNull();
    verify(users).save(user);
    verify(tokens).invalidateActiveTokens(eq(7), any(Date.class));
    verify(events).publishEvent(any(PasswordResetCompletedMailEvent.class));
  }

  @Test
  void resetPasswordRejectsMismatchMissingExpiredUsedAndReusedPassword() {
    assertThatThrownBy(() -> service.resetPassword(
        new ResetPasswordRequest("token", "New@1234", "Other@1234")))
        .isInstanceOf(IllegalArgumentException.class);

    when(tokens.lockByTokenHash(any())).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.resetPassword(
        new ResetPasswordRequest("missing", "New@1234", "New@1234")))
        .isInstanceOf(IllegalArgumentException.class);

    User user = activeUser();
    user.setPasswordHash("old-hash");
    PasswordResetToken expired = resetToken(user, new Date(System.currentTimeMillis() - 1_000));
    when(tokens.lockByTokenHash(any())).thenReturn(Optional.of(expired));
    assertThatThrownBy(() -> service.resetPassword(
        new ResetPasswordRequest("expired", "New@1234", "New@1234")))
        .isInstanceOf(IllegalArgumentException.class);

    PasswordResetToken used = resetToken(user, new Date(System.currentTimeMillis() + 60_000));
    used.setUsedAt(new Date());
    when(tokens.lockByTokenHash(any())).thenReturn(Optional.of(used));
    assertThatThrownBy(() -> service.resetPassword(
        new ResetPasswordRequest("used", "New@1234", "New@1234")))
        .isInstanceOf(IllegalArgumentException.class);

    PasswordResetToken reusable = resetToken(user, new Date(System.currentTimeMillis() + 60_000));
    when(tokens.lockByTokenHash(any())).thenReturn(Optional.of(reusable));
    when(passwordEncoder.matches("New@1234", "old-hash")).thenReturn(true);
    assertThatThrownBy(() -> service.resetPassword(
        new ResetPasswordRequest("reuse", "New@1234", "New@1234")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private User activeUser() {
    User user = new User(7);
    user.setEmail("student@example.com");
    user.setFullName("Student One");
    user.setStatus(AccountStatus.ACTIVE);
    return user;
  }

  private PasswordResetToken resetToken(User user, Date expiresAt) {
    PasswordResetToken token = new PasswordResetToken();
    token.setUserId(user);
    token.setTokenHash("hash");
    token.setCreatedAt(new Date());
    token.setExpiresAt(expiresAt);
    return token;
  }
}
