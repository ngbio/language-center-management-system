package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_attempt_answer")
@Getter
@Setter
public class QuizAttemptAnswer {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "attempt_id")
  private QuizAttempt attempt;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id")
  private QuizQuestion question;
  @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "selected_option_id")
  private QuizOption selectedOption;
  @Column(name = "is_correct")
  private boolean correct;
  @Column(precision = 6, scale = 2)
  private BigDecimal pointsAwarded;
}
