package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.ResetPasswordRequest;
import com.ntt.language_center_management.entity.PasswordResetToken;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.event.PasswordResetCompletedMailEvent;
import com.ntt.language_center_management.event.PasswordResetRequestedMailEvent;
import com.ntt.language_center_management.repository.PasswordResetTokenRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.PasswordResetService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {
  private static final String INVALID_TOKEN_MESSAGE =
      "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn";

  private final UserRepository users;
  private final PasswordResetTokenRepository tokens;
  private final PasswordEncoder passwordEncoder;
  private final ApplicationEventPublisher events;
  private final SecureRandom secureRandom = new SecureRandom();
  private final String frontendUrl;
  private final int expirationMinutes;
  private final int cooldownSeconds;

  public PasswordResetServiceImpl(
      UserRepository users,
      PasswordResetTokenRepository tokens,
      PasswordEncoder passwordEncoder,
      ApplicationEventPublisher events,
      @Value("${app.password-reset.frontend-url:http://localhost:5173}") String frontendUrl,
      @Value("${app.password-reset.expiration-minutes:30}") int expirationMinutes,
      @Value("${app.password-reset.cooldown-seconds:60}") int cooldownSeconds) {
    if (!StringUtils.hasText(frontendUrl)) {
      throw new IllegalArgumentException("APP_FRONTEND_URL không được để trống");
    }
    if (expirationMinutes < 5 || expirationMinutes > 120) {
      throw new IllegalArgumentException("Thời hạn reset password phải từ 5 đến 120 phút");
    }
    if (cooldownSeconds < 0) {
      throw new IllegalArgumentException("Thời gian chờ reset password không hợp lệ");
    }
    this.users = users;
    this.tokens = tokens;
    this.passwordEncoder = passwordEncoder;
    this.events = events;
    this.frontendUrl = frontendUrl.replaceAll("/+$", "");
    this.expirationMinutes = expirationMinutes;
    this.cooldownSeconds = cooldownSeconds;
  }

  @Override
  public void requestReset(String email) {
    String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
    User user = users.findByEmailIgnoreCase(normalizedEmail).orElse(null);
    if (user == null || user.getStatus() != AccountStatus.ACTIVE) return;

    Date now = new Date();
    Date cooldownStart = new Date(now.getTime() - cooldownSeconds * 1000L);
    if (tokens.existsByUserId_IdAndCreatedAtAfterAndUsedAtIsNull(user.getId(), cooldownStart)) {
      return;
    }

    tokens.invalidateActiveTokens(user.getId(), now);
    String rawToken = generateToken();
    PasswordResetToken token = new PasswordResetToken();
    token.setUserId(user);
    token.setTokenHash(hash(rawToken));
    token.setCreatedAt(now);
    token.setExpiresAt(new Date(now.getTime() + expirationMinutes * 60_000L));
    tokens.save(token);

    events.publishEvent(new PasswordResetRequestedMailEvent(
        user.getEmail(), user.getFullName(),
        frontendUrl + "/reset-password?token=" + rawToken, expirationMinutes));
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isTokenValid(String rawToken) {
    if (!StringUtils.hasText(rawToken)) return false;
    Date now = new Date();
    return tokens.findByTokenHash(hash(rawToken.trim()))
        .filter(token -> token.getUsedAt() == null)
        .filter(token -> token.getExpiresAt().after(now))
        .isPresent();
  }

  @Override
  public void resetPassword(ResetPasswordRequest request) {
    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new IllegalArgumentException("Xác nhận mật khẩu mới không khớp");
    }
    PasswordResetToken token = tokens.lockByTokenHash(hash(request.token().trim()))
        .orElseThrow(() -> new IllegalArgumentException(INVALID_TOKEN_MESSAGE));
    Date now = new Date();
    if (token.getUsedAt() != null || !token.getExpiresAt().after(now)) {
      throw new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
    }

    User user = token.getUserId();
    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException(INVALID_TOKEN_MESSAGE);
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
      throw new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại");
    }

    user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    user.setPasswordChangedAt(now);
    user.setUpdatedAt(now);
    token.setUsedAt(now);
    users.save(user);
    tokens.save(token);
    tokens.invalidateActiveTokens(user.getId(), now);
    events.publishEvent(new PasswordResetCompletedMailEvent(user.getEmail(), user.getFullName()));
  }

  private String generateToken() {
    byte[] bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      byte[] digest = MessageDigest.getInstance("SHA-256")
          .digest(rawToken.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("JVM không hỗ trợ SHA-256", exception);
    }
  }
}
