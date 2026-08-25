package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.LanguageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/languages")
public class LanguageApiController {
  private final LanguageService service;

  public LanguageApiController(LanguageService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<?> list() {
    return new ApiResponse<>(200, "Lấy danh sách ngôn ngữ thành công", service.getLanguages());
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy ngôn ngữ thành công", service.getById(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody LanguageRequest r) {
    r.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(201, "Tạo ngôn ngữ thành công", service.addOrUpdateLanguage(r)));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(@PathVariable Integer id, @Valid @RequestBody LanguageRequest r) {
    r.setId(id);
    return new ApiResponse<>(200, "Cập nhật ngôn ngữ thành công", service.addOrUpdateLanguage(r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    if (!service.deleteLanguage(id))
      throw new IllegalArgumentException("Không thể xóa ngôn ngữ đã có trình độ");
    return new ApiResponse<>(200, "Xóa ngôn ngữ thành công", null);
  }
}
