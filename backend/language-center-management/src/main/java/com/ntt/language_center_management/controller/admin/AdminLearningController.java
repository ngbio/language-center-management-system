package com.ntt.language_center_management.controller.admin;

import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.LearningService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/learning")
@RequiredArgsConstructor
public class AdminLearningController {
  private final LearningService service;
  private <T> ApiResponse<T> ok(T value) { return new ApiResponse<>(200, "Thành công", value); }

  @GetMapping("/contents/{id}/flashcards") public ApiResponse<List<LearningResponse.Card>> cards(@PathVariable Integer id) { return ok(service.adminCards(id)); }
  @PostMapping("/contents/{id}/flashcards") public ApiResponse<LearningResponse.Card> createCard(@PathVariable Integer id, @Valid @RequestBody LearningRequest.Card r) { return ok(service.saveCard(id,null,r)); }
  @PutMapping("/flashcards/{id}") public ApiResponse<LearningResponse.Card> updateCard(@PathVariable Long id, @Valid @RequestBody LearningRequest.Card r) { return ok(service.saveCard(null,id,r)); }
  @DeleteMapping("/flashcards/{id}") public ApiResponse<Void> deleteCard(@PathVariable Long id) { service.deactivateCard(id); return ok(null); }
  @PutMapping("/contents/{id}/flashcards/order") public ApiResponse<Void> reorder(@PathVariable Integer id, @Valid @RequestBody LearningRequest.Order r) { service.reorderCards(id,r); return ok(null); }
  @GetMapping("/contents/{id}/quizzes") public ApiResponse<List<LearningResponse.AdminQuiz>> quizzes(@PathVariable Integer id) { return ok(service.adminQuizzes(id)); }
  @PostMapping("/contents/{id}/quizzes") public ApiResponse<LearningResponse.AdminQuiz> createQuiz(@PathVariable Integer id, @Valid @RequestBody LearningRequest.Quiz r) { return ok(service.saveQuiz(id,null,r)); }
  @PutMapping("/quizzes/{id}") public ApiResponse<LearningResponse.AdminQuiz> updateQuiz(@PathVariable Long id, @Valid @RequestBody LearningRequest.Quiz r) { return ok(service.saveQuiz(null,id,r)); }
  @DeleteMapping("/quizzes/{id}") public ApiResponse<Void> archive(@PathVariable Long id) { service.archiveQuiz(id); return ok(null); }
  @PostMapping("/quizzes/{id}/questions") public ApiResponse<LearningResponse.AdminQuiz> createQuestion(@PathVariable Long id, @Valid @RequestBody LearningRequest.Question r) { return ok(service.saveQuestion(id,null,r)); }
  @PutMapping("/questions/{id}") public ApiResponse<LearningResponse.AdminQuiz> updateQuestion(@PathVariable Long id, @Valid @RequestBody LearningRequest.Question r) { return ok(service.saveQuestion(null,id,r)); }
  @DeleteMapping("/questions/{id}") public ApiResponse<Void> deleteQuestion(@PathVariable Long id) { service.deleteQuestion(id); return ok(null); }
}
