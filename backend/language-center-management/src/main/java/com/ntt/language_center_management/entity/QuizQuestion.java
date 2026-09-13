package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz_question")
@Getter
@Setter
public class QuizQuestion {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "quiz_id")
  private Quiz quiz;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String questionText;
  
  private String questionType;
  @Column(columnDefinition = "TEXT")
  private String explanation;
  @Column(precision = 6, scale = 2)
  private BigDecimal points;
  
  private int displayOrder;
}
