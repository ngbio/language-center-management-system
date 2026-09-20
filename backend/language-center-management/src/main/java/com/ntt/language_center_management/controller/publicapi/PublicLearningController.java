package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.request.LearningRequest;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.LearningService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/learning")
@RequiredArgsConstructor
public class PublicLearningController {
  private final LearningService service;
  private <T> ApiResponse<T> ok(T value) { return new ApiResponse<>(200, "Thành công", value); }

  private final com.ntt.language_center_management.service.impl.PublicQuizRateLimiter limiter;
  @GetMapping("/courses/{id}") public ApiResponse<LearningResponse.Course> course(@PathVariable Integer id) { return ok(service.course(id,null)); }
  @GetMapping("/contents/{id}/flashcards") public ApiResponse<List<LearningResponse.Card>> cards(@PathVariable Integer id) { return ok(service.cards(id,null)); }
  @GetMapping("/contents/{id}/quizzes") public ApiResponse<List<LearningResponse.Quiz>> quizzes(@PathVariable Integer id) { return ok(service.quizzes(id,null)); }
  @PostMapping("/quizzes/{id}/evaluate")
  public org.springframework.http.ResponseEntity<ApiResponse<LearningResponse.Result>> evaluate(
      @PathVariable Long id, @Valid @RequestBody LearningRequest.Submission r,
      jakarta.servlet.http.HttpServletRequest request) {
    if (!limiter.allow(request.getRemoteAddr()))
      return org.springframework.http.ResponseEntity.status(429).header("Retry-After","60")
          .body(new ApiResponse<>(429, "Bạn gửi bài quá nhanh. Vui lòng thử lại sau một phút.", null));
    return org.springframework.http.ResponseEntity.ok(ok(service.evaluate(id,r)));
  }
}
