package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.service.LanguageService;
import com.ntt.language_center_management.service.LevelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/languages")
public class LanguageApiController {
  private final LanguageService languageService;
  private final LevelService levelService;

  public LanguageApiController(LanguageService languageService, LevelService levelService) {
    this.languageService = languageService;
    this.levelService = levelService;
  }

  @GetMapping
  public ApiResponse<?> list() {
    return new ApiResponse<>(
        200, "Lấy danh sách ngôn ngữ thành công", languageService.getActiveLanguages());
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(
        200, "Lấy ngôn ngữ thành công", languageService.getActiveById(id));
  }

  @GetMapping("/{id}/levels")
  public ApiResponse<?> getLevels(@PathVariable Integer id) {
    return new ApiResponse<>(
        200, "Lấy danh sách trình độ thành công", levelService.getActive(id));
  }
}
