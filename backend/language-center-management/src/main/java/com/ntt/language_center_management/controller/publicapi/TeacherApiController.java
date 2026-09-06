package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import com.ntt.language_center_management.service.TeacherService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers")
public class TeacherApiController {

  private final TeacherService teacherService;

  public TeacherApiController(TeacherService teacherService) {
    this.teacherService = teacherService;
  }

  @GetMapping
  public ApiResponse<List<TeacherOptionResponse>> getActiveTeachers() {
    return new ApiResponse<>(
        200, "Lấy danh sách giảng viên thành công", teacherService.getActiveTeachers());
  }
}
