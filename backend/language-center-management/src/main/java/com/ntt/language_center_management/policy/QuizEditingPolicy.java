package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.repository.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class QuizEditingPolicy {
  private final QuizAttemptRepository attempts;
  private final QuizQuestionRepository questions;
  private final QuizOptionRepository options;
  public QuizEditingPolicy(QuizAttemptRepository attempts, QuizQuestionRepository questions, QuizOptionRepository options) {
    this.attempts = attempts;
    this.questions = questions;
    this.options = options;
  }

  public void editable(Quiz quiz) {
    if (!"DRAFT".equals(quiz.getStatus()) || attempts.existsByQuiz_Id(quiz.getId()))
      throw new IllegalArgumentException("Chỉ sửa câu hỏi của quiz nháp chưa có lượt làm. Hãy tạo quiz mới.");
  }
  public void validQuestion(String type, List<Boolean> correct) {
    if (correct.size() < 2 || correct.stream().filter(Boolean::booleanValue).count() != 1
        || ("TRUE_FALSE".equals(type) && correct.size() != 2))
      throw new IllegalArgumentException("Mỗi câu cần đúng một đáp án đúng; câu đúng/sai cần đúng hai lựa chọn");
  }
  public void validateConfiguration(Quiz quiz, Long id, LearningRequest.Quiz request) {
    if (id != null && attempts.existsByQuiz_Id(id)
        && (quiz.getPassingScore().compareTo(request.passingScore()) != 0
            || !Objects.equals(quiz.getMaxAttempts(), request.maxAttempts())))
      throw new IllegalArgumentException("Không thay đổi thang đạt hoặc giới hạn sau khi có lượt làm");
    if ("PUBLISHED".equals(request.status())) {
      var items = id == null ? List.<QuizQuestion>of() : questions.findByQuiz_IdOrderByDisplayOrderAsc(id);
      if (items.isEmpty()) throw new IllegalArgumentException("Hãy thêm câu hỏi trước khi xuất bản");
      for (var q : items) validQuestion(q.getQuestionType(),
          options.findByQuestion_IdOrderByDisplayOrderAsc(q.getId()).stream().map(QuizOption::isCorrect).toList());
    }
  }
  public void validateQuestionCapacity(Quiz quiz) {
      if (questions.findByQuiz_IdOrderByDisplayOrderAsc(quiz.getId()).size() >= 200)
        throw new IllegalArgumentException("Quiz tối đa 200 câu");
  }

}
