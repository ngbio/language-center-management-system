package com.ntt.language_center_management.learning;

import org.springframework.stereotype.Component;

@Component
public class FixedIntervalReviewStrategy implements ReviewSchedulingStrategy {
  @Override
  public int intervalDays(String masteryLevel) {
    return switch (masteryLevel) {
      case "AGAIN" -> 1;
      case "HARD" -> 3;
      case "REMEMBERED" -> 7;
      default -> throw new IllegalArgumentException("Mức độ nhớ không hợp lệ");
    };
  }
}
