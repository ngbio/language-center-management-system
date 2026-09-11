package com.ntt.language_center_management.unit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ntt.language_center_management.util.JwtUtils;
import org.junit.jupiter.api.Test;

class JwtUtilsTest {
  private static final String SECRET = "0123456789abcdef0123456789abcdef";

  @Test
  void shouldGenerateTokenAndParseSubject() {
    JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000);

    String token = jwtUtils.generateToken("student@example.com");

    assertThat(token).isNotBlank();
    assertThat(jwtUtils.validateTokenAndGetUsername(token)).isEqualTo("student@example.com");
  }

  @Test
  void shouldRejectInvalidConfigurationAndBlankInputs() {
    assertThatThrownBy(() -> new JwtUtils("too-short", 60_000))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new JwtUtils(SECRET, 0))
        .isInstanceOf(IllegalArgumentException.class);

    JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000);
    assertThatThrownBy(() -> jwtUtils.generateToken("  "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> jwtUtils.validateTokenAndGetUsername(null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectExpiredMalformedAndTamperedTokens() throws Exception {
    JwtUtils shortLived = new JwtUtils(SECRET, 1);
    String expired = shortLived.generateToken("student@example.com");
    Thread.sleep(10);
    assertThatThrownBy(() -> shortLived.validateTokenAndGetUsername(expired))
        .isInstanceOf(IllegalArgumentException.class);

    JwtUtils jwtUtils = new JwtUtils(SECRET, 60_000);
    assertThatThrownBy(() -> jwtUtils.validateTokenAndGetUsername("not-a-jwt"))
        .isInstanceOf(IllegalArgumentException.class);
    String valid = jwtUtils.generateToken("student@example.com");
    String tampered = valid.substring(0, valid.length() - 1)
        + (valid.endsWith("a") ? "b" : "a");
    assertThatThrownBy(() -> jwtUtils.validateTokenAndGetUsername(tampered))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
