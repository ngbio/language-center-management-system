package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.request.ChangeCatalogStatusRequest;
import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.service.LevelService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/levels")
public class AdminLevelApiController {
  private final LevelService levelService;

  public AdminLevelApiController(LevelService levelService) {
    this.levelService = levelService;
  }

  @GetMapping
  public ApiResponse<?> list(
      @RequestParam(required = false) Integer languageId,
      @RequestParam(required = false) String status) {
    return new ApiResponse<>(
        200, "Lấy danh sách trình độ thành công", levelService.getAll(languageId, status));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy trình độ thành công", levelService.getById(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody LevelRequest request) {
    request.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(201, "Tạo trình độ thành công", levelService.save(request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(
      @PathVariable Integer id, @Valid @RequestBody LevelRequest request) {
    request.setId(id);
    return new ApiResponse<>(200, "Cập nhật trình độ thành công", levelService.save(request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<?> changeStatus(
      @PathVariable Integer id, @Valid @RequestBody ChangeCatalogStatusRequest request) {
    return new ApiResponse<>(
        200, "Cập nhật trạng thái trình độ thành công",
        levelService.changeStatus(id, request.status()));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    levelService.delete(id);
    return new ApiResponse<>(200, "Xóa trình độ thành công", null);
  }
}
