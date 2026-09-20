package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "flashcard_review")
@Getter
@Setter
public class FlashcardReview {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id")
  private Student student;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "flashcard_id")
  private Flashcard flashcard;
  
  private String masteryLevel;
  
  private int repetitionCount;
  
  private int intervalDays;
  
  private LocalDateTime lastReviewedAt;
  
  private LocalDateTime nextReviewAt;
}
