package com.ntt.language_center_management.learning;

import java.math.BigDecimal;
import java.util.List;

public interface QuestionGradingStrategy {
  record Option(Long id, boolean correct) {}
  record Grade(Long correctOptionId, boolean correct, BigDecimal points) {}

  Grade grade(BigDecimal points, List<Option> options, Long selectedOptionId);
}
