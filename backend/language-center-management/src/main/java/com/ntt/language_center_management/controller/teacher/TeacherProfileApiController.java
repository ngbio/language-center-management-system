package com.ntt.language_center_management.controller.teacher;

import com.ntt.language_center_management.dto.request.TeacherProfileUpdateRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.TeacherProfileResponse;
import com.ntt.language_center_management.service.TeacherService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers/me/profile")
public class TeacherProfileApiController {

  private final TeacherService teacherService;

  public TeacherProfileApiController(TeacherService teacherService) {
    this.teacherService = teacherService;
  }

  @GetMapping
  public ApiResponse<TeacherProfileResponse> getProfile(Principal principal) {
    return new ApiResponse<>(
        200, "Lấy hồ sơ giảng viên thành công", teacherService.getProfile(principal));
  }

  @PutMapping
  public ApiResponse<TeacherProfileResponse> updateProfile(
      @Valid @RequestBody TeacherProfileUpdateRequest request, Principal principal) {
    return new ApiResponse<>(
        200, "Cập nhật hồ sơ giảng viên thành công", teacherService.updateProfile(principal, request));
  }
}
