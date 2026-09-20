package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ntt.language_center_management.service.impl.PublicQuizRateLimiter;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class PublicQuizRateLimiterTest {
  @Test void limitsByAddressAndResetsAfterMinute() {
    var clock = mock(Clock.class);
    when(clock.millis()).thenReturn(60000L);
    var limiter = new PublicQuizRateLimiter(clock);
    for (int i = 0; i < 20; i++) assertTrue(limiter.allow("127.0.0.1"));
    assertFalse(limiter.allow("127.0.0.1"));
    assertTrue(limiter.allow("127.0.0.2"));
    when(clock.millis()).thenReturn(120000L);
    assertTrue(limiter.allow("127.0.0.1"));
  }
}
