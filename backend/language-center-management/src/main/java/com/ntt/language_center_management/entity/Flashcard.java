package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "flashcard")
@Getter
@Setter
public class Flashcard {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "course_content_id")
  private CourseContent content;
  @Column(nullable = false, length = 500)
  private String frontText;
  @Column(nullable = false, columnDefinition = "TEXT")
  private String backText;
  @Column(columnDefinition = "TEXT")
  private String exampleSentence;
  
  private int displayOrder;
  @Column(nullable = false, length = 20)
  private String status;
}
