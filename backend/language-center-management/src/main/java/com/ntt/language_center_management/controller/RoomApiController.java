package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.request.RoomRequest;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
public class RoomApiController {
  private final RoomService service;

  public RoomApiController(RoomService service) {
    this.service = service;
  }

  @GetMapping
  public ApiResponse<?> list() {
    return new ApiResponse<>(200, "Lấy danh sách phòng thành công", service.getAll());
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy phòng thành công", service.getById(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody RoomRequest r) {
    r.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(201, "Tạo phòng thành công", service.save(r)));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(@PathVariable Integer id, @Valid @RequestBody RoomRequest r) {
    r.setId(id);
    return new ApiResponse<>(200, "Cập nhật phòng thành công", service.save(r));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    service.delete(id);
    return new ApiResponse<>(200, "Xóa phòng thành công", null);
  }
}
