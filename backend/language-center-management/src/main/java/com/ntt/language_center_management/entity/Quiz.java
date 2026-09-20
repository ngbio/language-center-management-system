package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "quiz")
@Getter
@Setter
public class Quiz {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_content_id")
  private CourseContent content;
  @Column(nullable = false, length = 255)
  private String title;
  @Column(precision = 5, scale = 2)
  private BigDecimal passingScore;
  
  private Integer maxAttempts;
  @Column(nullable = false, length = 20)
  private String status;
}
