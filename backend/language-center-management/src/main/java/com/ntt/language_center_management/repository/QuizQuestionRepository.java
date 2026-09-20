package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.QuizQuestion;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
  List<QuizQuestion> findByQuiz_IdOrderByDisplayOrderAsc(Long quizId);
}
