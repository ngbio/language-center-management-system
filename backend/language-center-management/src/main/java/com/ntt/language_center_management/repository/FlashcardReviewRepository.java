package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.FlashcardReview;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FlashcardReviewRepository extends JpaRepository<FlashcardReview, Long> {
  Optional<FlashcardReview> findByStudent_IdAndFlashcard_Id(Integer studentId, Long cardId);
  List<FlashcardReview> findByStudent_IdAndNextReviewAtLessThanEqualOrderByNextReviewAtAsc(Integer studentId, LocalDateTime now);
}
