package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.admin.AdminLearningController;
import com.ntt.language_center_management.controller.publicapi.PublicLearningController;
import com.ntt.language_center_management.controller.student.StudentLearningController;
import com.ntt.language_center_management.dto.response.LearningResponse;
import com.ntt.language_center_management.service.LearningService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.service.impl.PublicQuizRateLimiter;
import com.ntt.language_center_management.util.JwtUtils;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({PublicLearningController.class,StudentLearningController.class,AdminLearningController.class})
@Import(SecurityConfig.class)
class LearningControllerIntegrationTest {
  @Autowired MockMvc mvc;
  @MockitoBean LearningService service;
  @MockitoBean PublicQuizRateLimiter limiter;
  @MockitoBean UserService users;
  @MockitoBean JwtUtils jwt;

  @Test void publicCourseAllowsGuest() throws Exception {
    when(service.course(1,null)).thenReturn(new LearningResponse.Course(1,"Free",List.of()));
    mvc.perform(get("/api/public/learning/courses/1")).andExpect(status().isOk());
    verify(service).course(1,null);
  }
  @Test void publicQuizDoesNotExposeCorrectAnswerOrExplanation() throws Exception {
    when(service.quizzes(3,null)).thenReturn(List.of(new LearningResponse.Quiz(1L,"Quiz",BigDecimal.TEN,null,
        "PUBLISHED",List.of(new LearningResponse.Question(2L,"Question","SINGLE_CHOICE",BigDecimal.ONE,
        List.of(new LearningResponse.Option(3L,"Option")))))));
    mvc.perform(get("/api/public/learning/contents/3/quizzes")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].questions[0].explanation").doesNotExist())
        .andExpect(jsonPath("$.data[0].questions[0].options[0].correct").doesNotExist())
        .andExpect(jsonPath("$.data[0].questions[0].options[0].isCorrect").doesNotExist());
  }
  @Test void publicEvaluationAllowedWithoutCsrfOrLogin() throws Exception {
    when(limiter.allow(anyString())).thenReturn(true);
    when(service.evaluate(eq(1L),any())).thenReturn(new LearningResponse.Result(null,BigDecimal.ZERO,false,List.of()));
    mvc.perform(post("/api/public/learning/quizzes/1/evaluate").contentType(MediaType.APPLICATION_JSON)
        .content("{\"answers\":[]}")).andExpect(status().isOk());
  }
  @Test void publicEvaluationIsRateLimited() throws Exception {
    mvc.perform(post("/api/public/learning/quizzes/1/evaluate").contentType(MediaType.APPLICATION_JSON)
        .content("{\"answers\":[]}")).andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After","60"));
    verifyNoInteractions(service);
  }
  @Test void guestCannotSaveReview() throws Exception {
    mvc.perform(put("/api/students/me/learning/flashcards/1/review")
        .contentType(MediaType.APPLICATION_JSON).content("{\"masteryLevel\":\"HARD\"}"))
        .andExpect(status().isUnauthorized());
  }
  @Test @WithMockUser(roles="TEACHER") void teacherCannotUseStudentHistory() throws Exception {
    mvc.perform(get("/api/students/me/learning/quizzes/1/attempts")).andExpect(status().isForbidden());
  }
  @Test @WithMockUser(roles="STUDENT") void studentCannotReadAdminAnswers() throws Exception {
    mvc.perform(get("/api/admin/learning/contents/1/quizzes")).andExpect(status().isForbidden());
  }
  @Test @WithMockUser(roles="STUDENT") void studentCanSaveReview() throws Exception {
    mvc.perform(put("/api/students/me/learning/flashcards/1/review")
        .contentType(MediaType.APPLICATION_JSON).content("{\"masteryLevel\":\"HARD\"}")).andExpect(status().isOk());
    verify(service).review(eq(1L),any(),any());
  }
  @Test @WithMockUser(roles="STUDENT") void invalidReviewIsRejected() throws Exception {
    mvc.perform(put("/api/students/me/learning/flashcards/1/review")
        .contentType(MediaType.APPLICATION_JSON).content("{\"masteryLevel\":\"HACK\"}")).andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
  @Test @WithMockUser(roles="ADMIN") void adminCanCreateDraft() throws Exception {
    mvc.perform(post("/api/admin/learning/contents/1/quizzes").contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"Quiz\",\"passingScore\":50,\"maxAttempts\":null,\"status\":\"DRAFT\"}"))
        .andExpect(status().isOk());
  }
  @Test @WithMockUser(roles="ADMIN") void blankAndOutOfRangeQuizRejected() throws Exception {
    mvc.perform(post("/api/admin/learning/contents/1/quizzes").contentType(MediaType.APPLICATION_JSON)
        .content("{\"title\":\"\",\"passingScore\":101,\"status\":\"DRAFT\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
  @Test @WithMockUser(roles="ADMIN") void dataConstraintReturnsConflictWithoutDatabaseDetails() throws Exception {
    doThrow(new org.springframework.dao.DataIntegrityViolationException("internal constraint details"))
        .when(service).archiveQuiz(1L);
    mvc.perform(delete("/api/admin/learning/quizzes/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("internal constraint details"))));
  }
}
