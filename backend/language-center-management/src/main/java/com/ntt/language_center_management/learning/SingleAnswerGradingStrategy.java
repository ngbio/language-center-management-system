package com.ntt.language_center_management.learning;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/** SINGLE_CHOICE and TRUE_FALSE currently share the same one-correct-answer scoring rule. */
@Component
public class SingleAnswerGradingStrategy implements QuestionGradingStrategy {
  @Override
  public Grade grade(BigDecimal points, List<Option> options, Long selectedOptionId) {
    var correct = options.stream().filter(Option::correct).toList();
    if (correct.size() != 1) throw new IllegalArgumentException("Quiz có đáp án không hợp lệ");
    var selected = selectedOptionId == null ? null : options.stream()
        .filter(option -> option.id().equals(selectedOptionId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Lựa chọn không thuộc câu hỏi"));
    boolean isCorrect = selected != null && selected.correct();
    return new Grade(correct.getFirst().id(), isCorrect, isCorrect ? points : BigDecimal.ZERO);
  }
}
