package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.*;
import com.ntt.language_center_management.learning.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearningStrategiesTest {
  @Test
  void reviewKeepsOneThreeSevenDayIntervals() {
    var strategy = new FixedIntervalReviewStrategy();
    assertThat(strategy.intervalDays("AGAIN")).isEqualTo(1);
    assertThat(strategy.intervalDays("HARD")).isEqualTo(3);
    assertThat(strategy.intervalDays("REMEMBERED")).isEqualTo(7);
    assertThatThrownBy(() -> strategy.intervalDays("UNKNOWN")).hasMessage("Mức độ nhớ không hợp lệ");
  }

  @Test
  void singleAnswerScoringKeepsCorrectWrongAndUnansweredResults() {
    var strategy = new SingleAnswerGradingStrategy();
    var options = List.of(new QuestionGradingStrategy.Option(1L, true), new QuestionGradingStrategy.Option(2L, false));
    assertThat(strategy.grade(BigDecimal.TEN, options, 1L).points()).isEqualTo(BigDecimal.TEN);
    assertThat(strategy.grade(BigDecimal.TEN, options, 2L).points()).isEqualTo(BigDecimal.ZERO);
    assertThat(strategy.grade(BigDecimal.TEN, options, null).points()).isEqualTo(BigDecimal.ZERO);
    assertThatThrownBy(() -> strategy.grade(BigDecimal.TEN, options, 99L)).hasMessage("Lựa chọn không thuộc câu hỏi");
    assertThatThrownBy(() -> strategy.grade(BigDecimal.TEN,
        List.of(new QuestionGradingStrategy.Option(1L, true), new QuestionGradingStrategy.Option(2L, true)), 1L))
        .hasMessage("Quiz có đáp án không hợp lệ");
  }
}
