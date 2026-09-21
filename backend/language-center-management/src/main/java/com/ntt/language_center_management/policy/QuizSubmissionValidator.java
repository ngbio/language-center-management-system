package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.repository.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class QuizSubmissionValidator {

  public Map<Long, Long> validateAnswers(List<QuizQuestion> items, LearningRequest.Submission request) {
    if (items.isEmpty()) throw new IllegalArgumentException("Quiz chưa có câu hỏi");
    Map<Long, Long> selected = new HashMap<>();
    Set<Long> questionIds = items.stream().map(QuizQuestion::getId).collect(Collectors.toSet());
    for (var answer : request.answers()) {
      if (!questionIds.contains(answer.questionId()) || selected.containsKey(answer.questionId()))
        throw new IllegalArgumentException("Câu hỏi bị trùng hoặc không thuộc quiz");
      selected.put(answer.questionId(), answer.selectedOptionId());
    }
    return selected;
  }
  public void validateTotal(BigDecimal total) {
    if (total.signum() <= 0) throw new IllegalArgumentException("Tổng điểm phải lớn hơn 0");
  }

}
