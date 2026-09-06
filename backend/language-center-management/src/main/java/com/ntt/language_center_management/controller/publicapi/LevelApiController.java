package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.service.LevelService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/levels")
public class LevelApiController {
  private final LevelService levelService;

  public LevelApiController(LevelService levelService) {
    this.levelService = levelService;
  }

  @GetMapping
  public ApiResponse<?> list() {
    return new ApiResponse<>(
        200, "Lấy danh sách trình độ thành công", levelService.getActive(null));
  }

  @GetMapping("/{id}")
  public ApiResponse<?> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy trình độ thành công", levelService.getActiveById(id));
  }
}
