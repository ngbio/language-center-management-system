package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_attempt")
@Getter
@Setter
public class QuizAttempt {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "student_id")
  private Student student;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quiz_id")
  private Quiz quiz;
  
  private int attemptNumber;
  @Column(precision = 5, scale = 2)
  private BigDecimal score;
  
  private Boolean passed;
  
  private LocalDateTime startedAt;
  
  private LocalDateTime submittedAt;
}
