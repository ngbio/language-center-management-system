package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Quiz;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
  List<Quiz> findByContent_IdOrderByIdAsc(Integer contentId);
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select q from Quiz q where q.id = :id")
  Optional<Quiz> lockById(@Param("id") Long id);
}
