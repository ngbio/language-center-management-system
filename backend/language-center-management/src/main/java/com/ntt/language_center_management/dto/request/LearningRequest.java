package com.ntt.language_center_management.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public final class LearningRequest {
  private LearningRequest() {}
  public record Card(@NotBlank @Size(max=500) String frontText,
      @NotBlank @Size(max=16000) String backText, @Size(max=16000) String exampleSentence,
      @NotBlank @Pattern(regexp="ACTIVE|INACTIVE") String status) {}
  public record Review(@NotBlank @Pattern(regexp="AGAIN|HARD|REMEMBERED") String masteryLevel) {}
  public record Option(@NotBlank @Size(max=4000) String optionText, boolean correct) {}
  public record Question(@NotBlank @Size(max=16000) String questionText,
      @NotBlank @Pattern(regexp="SINGLE_CHOICE|TRUE_FALSE") String questionType,
      @Size(max=16000) String explanation,
      @NotNull @DecimalMin("0.01") @DecimalMax("9999.99") @Digits(integer=4,fraction=2) BigDecimal points,
      @NotNull @Size(min=2,max=10) List<@NotNull @Valid Option> options) {}
  public record Quiz(@NotBlank @Size(max=255) String title,
      @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal passingScore,
      @Min(1) @Max(1000) Integer maxAttempts,
      @NotBlank @Pattern(regexp="DRAFT|PUBLISHED|ARCHIVED") String status) {}
  public record Answer(@NotNull @Positive Long questionId, @Positive Long selectedOptionId) {}
  public record Submission(@NotNull @Size(max=200) List<@NotNull @Valid Answer> answers) {}
  public record Order(@NotEmpty @Size(max=1000) List<@NotNull @Positive Long> ids) {}
}
