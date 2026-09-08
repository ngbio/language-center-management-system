package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.service.RoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
