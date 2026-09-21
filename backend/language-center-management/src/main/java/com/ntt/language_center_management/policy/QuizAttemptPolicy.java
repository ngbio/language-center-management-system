package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.repository.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class QuizAttemptPolicy {

  public void validateLimit(Quiz quiz, int attemptCount) {
    if (quiz.getMaxAttempts() != null && attemptCount >= quiz.getMaxAttempts())
      throw new IllegalArgumentException("Bạn đã sử dụng hết số lần làm quiz");
  }
  public void validateOwner(QuizAttempt attempt, Student student) {
    if (!attempt.getStudent().getId().equals(student.getId())) throw new ForbiddenException("Lượt làm không thuộc về bạn");
  }
  public void validateNotSubmitted(QuizAttempt attempt) {
    if (attempt.getSubmittedAt() != null) throw new IllegalArgumentException("Lượt làm này đã được nộp");
  }

}
