package com.ntt.language_center_management.service.impl;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Bounded, per-instance public practice limit; does not trust forwarded client IP headers. */
@Component
public class PublicQuizRateLimiter {
  private record Window(long minute, int count) {}
  private final Map<String, Window> windows = new HashMap<>();
  private final Clock clock;
  public PublicQuizRateLimiter() { this(Clock.systemUTC()); }
  public PublicQuizRateLimiter(Clock clock) { this.clock = clock; }
  public synchronized boolean allow(String address) {
    long minute = clock.millis() / 60000;
    windows.entrySet().removeIf(entry -> entry.getValue().minute() != minute);
    var window = windows.get(address);
    if (window == null && windows.size() >= 10000) return false;
    int count = window == null ? 0 : window.count();
    if (count >= 20) return false;
    windows.put(address, new Window(minute, count + 1));
    return true;
  }
}
