package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Flashcard;
import java.util.*;
import java.time.LocalDateTime;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface FlashcardRepository extends JpaRepository<Flashcard, Long> {
  List<Flashcard> findByContent_IdOrderByDisplayOrderAsc(Integer contentId);
}
