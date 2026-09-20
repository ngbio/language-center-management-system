package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_option")
@Getter
@Setter
public class QuizOption {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "question_id")
  private QuizQuestion question;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String optionText;
  @Column(name = "is_correct")
  private boolean correct;
  
  private int displayOrder;
}
