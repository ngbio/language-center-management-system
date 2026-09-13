package com.ntt.language_center_management.controller.student;

import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.LearningService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students/me/learning")
@RequiredArgsConstructor
public class StudentLearningController {
  private final LearningService service;
  private <T> ApiResponse<T> ok(T value) { return new ApiResponse<>(200, "Thành công", value); }

  @GetMapping("/courses/{id}") public ApiResponse<LearningResponse.Course> course(@PathVariable Integer id, Principal p) { return ok(service.course(id,p)); }
  @GetMapping("/contents/{id}/flashcards") public ApiResponse<List<LearningResponse.Card>> cards(@PathVariable Integer id, Principal p) { return ok(service.cards(id,p)); }
  @GetMapping("/flashcards/due") public ApiResponse<List<LearningResponse.Card>> due(Principal p) { return ok(service.due(p)); }
  @PutMapping("/flashcards/{id}/review") public ApiResponse<LearningResponse.Card> review(@PathVariable Long id, @Valid @RequestBody LearningRequest.Review r, Principal p) { return ok(service.review(id,r,p)); }
  @GetMapping("/contents/{id}/quizzes") public ApiResponse<List<LearningResponse.Quiz>> quizzes(@PathVariable Integer id, Principal p) { return ok(service.quizzes(id,p)); }
  @PostMapping("/quizzes/{id}/attempts") public ApiResponse<LearningResponse.Attempt> start(@PathVariable Long id, Principal p) { return ok(service.start(id,p)); }
  @PostMapping("/attempts/{id}/submit") public ApiResponse<LearningResponse.Result> submit(@PathVariable Long id, @Valid @RequestBody LearningRequest.Submission r, Principal p) { return ok(service.submit(id,r,p)); }
  @GetMapping("/quizzes/{id}/attempts") public ApiResponse<List<LearningResponse.Attempt>> history(@PathVariable Long id, Principal p) { return ok(service.history(id,p)); }
}
