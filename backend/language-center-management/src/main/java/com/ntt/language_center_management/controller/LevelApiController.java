package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.LevelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/levels")
public class LevelApiController {
  private final LevelService service;

  public LevelApiController(LevelService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<?> list(@RequestParam(required = false) Integer languageId) {
    return new ApiResponse<>(200, "Lấy danh sách trình độ thành công", service.getAll(languageId));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy trình độ thành công", service.getById(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody LevelRequest r) {
    r.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(201, "Tạo trình độ thành công", service.save(r)));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(@PathVariable Integer id, @Valid @RequestBody LevelRequest r) {
    r.setId(id);
    return new ApiResponse<>(200, "Cập nhật trình độ thành công", service.save(r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return new ApiResponse<>(200, "Xóa trình độ thành công", null);
  }
}
