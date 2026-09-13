package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.QuizOption;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface QuizOptionRepository extends JpaRepository<QuizOption, Long> {
  List<QuizOption> findByQuestion_IdOrderByDisplayOrderAsc(Long questionId);
}
