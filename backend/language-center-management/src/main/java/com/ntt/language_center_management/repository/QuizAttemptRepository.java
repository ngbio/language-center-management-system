package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.QuizAttempt;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
  List<QuizAttempt> findByStudent_IdAndQuiz_IdOrderByAttemptNumberDesc(Integer studentId, Long quizId);
  boolean existsByQuiz_Id(Long quizId);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from QuizAttempt a where a.id = :id")
  Optional<QuizAttempt> lockById(@Param("id") Long id);
}
