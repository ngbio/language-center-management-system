package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.service.RoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** @deprecated Public pages do not consume the room catalog. Use the protected admin API. */
@Deprecated(since = "2026-09", forRemoval = true)
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
}
