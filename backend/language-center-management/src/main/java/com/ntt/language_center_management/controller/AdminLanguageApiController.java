package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.request.ChangeCatalogStatusRequest;
import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.service.LanguageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/admin/languages")
public class AdminLanguageApiController {
  private final LanguageService languageService;

  public AdminLanguageApiController(LanguageService languageService) {
    this.languageService = languageService;
  }

  @GetMapping
  public ApiResponse<?> list(@RequestParam(required = false) String status) {
    return new ApiResponse<>(
        200, "Lấy danh sách ngôn ngữ thành công", languageService.getLanguages(status));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy ngôn ngữ thành công", languageService.getById(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody LanguageRequest request) {
    request.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new ApiResponse<>(
                201, "Tạo ngôn ngữ thành công", languageService.addOrUpdateLanguage(request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(
      @PathVariable Integer id, @Valid @RequestBody LanguageRequest request) {
    request.setId(id);
    return new ApiResponse<>(
        200, "Cập nhật ngôn ngữ thành công", languageService.addOrUpdateLanguage(request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<?> changeStatus(
      @PathVariable Integer id, @Valid @RequestBody ChangeCatalogStatusRequest request) {
    return new ApiResponse<>(
        200, "Cập nhật trạng thái ngôn ngữ thành công",
        languageService.changeStatus(id, request.status()));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    if (!languageService.deleteLanguage(id)) {
      throw new IllegalArgumentException("Không thể xóa ngôn ngữ đã có trình độ");
    }
    return new ApiResponse<>(200, "Xóa ngôn ngữ thành công", null);
  }
}
