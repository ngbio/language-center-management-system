package com.ntt.language_center_management.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class LearningResponse {
  private LearningResponse() {}
  public record Content(Integer id, String title, String summary, String contentHtml, String documentUrl) {}
  public record Section(Integer id, String title, List<Content> contents) {}
  public record Course(Integer id, String title, List<Section> sections) {}
  public record Card(Long id, Integer contentId, String frontText, String backText, String exampleSentence,
      int displayOrder, String status, String masteryLevel, LocalDateTime nextReviewAt) {}
  public record Option(Long id, String optionText) {}
  public record Question(Long id, String questionText, String questionType, BigDecimal points, List<Option> options) {}
  public record Quiz(Long id, String title, BigDecimal passingScore, Integer maxAttempts,
      String status, List<Question> questions) {}
  public record AdminOption(Long id, String optionText, boolean correct) {}
  public record AdminQuestion(Long id, String questionText, String questionType, String explanation,
      BigDecimal points, List<AdminOption> options) {}
  public record AdminQuiz(Long id, String title, BigDecimal passingScore, Integer maxAttempts,
      String status, boolean locked, List<AdminQuestion> questions) {}
  public record Answer(Long questionId, Long selectedOptionId, Long correctOptionId,
      boolean correct, BigDecimal pointsAwarded, String explanation) {}
  public record Result(Long attemptId, BigDecimal score, boolean passed, List<Answer> answers) {}
  public record Attempt(Long id, int attemptNumber, BigDecimal score, Boolean passed,
      LocalDateTime startedAt, LocalDateTime submittedAt, Quiz quiz, Result result) {}
}
