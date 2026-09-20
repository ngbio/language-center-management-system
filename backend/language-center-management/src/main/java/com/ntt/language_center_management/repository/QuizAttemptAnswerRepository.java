package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.QuizAttemptAnswer;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptAnswerRepository extends JpaRepository<QuizAttemptAnswer, Long> {
  List<QuizAttemptAnswer> findByAttempt_IdOrderByIdAsc(Long attemptId);
}
