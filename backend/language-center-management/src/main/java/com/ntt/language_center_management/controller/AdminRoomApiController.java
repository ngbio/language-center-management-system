package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.request.RoomRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rooms")
public class AdminRoomApiController {
  private final RoomService roomService;

  public AdminRoomApiController(RoomService roomService) {
    this.roomService = roomService;
  }

  @GetMapping
  public ApiResponse<?> list(@RequestParam(required = false) String status) {
    return new ApiResponse<>(
        200, "Lấy danh sách phòng thành công", roomService.getAll(status));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy phòng thành công", roomService.getById(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<?>> create(@Valid @RequestBody RoomRequest request) {
    request.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(201, "Tạo phòng thành công", roomService.save(request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<?> update(
      @PathVariable Integer id, @Valid @RequestBody RoomRequest request) {
    request.setId(id);
    return new ApiResponse<>(200, "Cập nhật phòng thành công", roomService.save(request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    roomService.delete(id);
    return new ApiResponse<>(200, "Xóa phòng thành công", null);
  }
}
