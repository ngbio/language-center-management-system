package com.ntt.language_center_management.controller.student;

import com.ntt.language_center_management.dto.request.StudentProfileUpdateRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.StudentProfileResponse;
import com.ntt.language_center_management.service.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/me/profile")
public class StudentProfileApiController {

  private final UserService userService;

  public StudentProfileApiController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public ApiResponse<StudentProfileResponse> getProfile(Principal principal) {
    return new ApiResponse<>(
        200, "Lấy hồ sơ học viên thành công", userService.getStudentProfile(principal));
  }

  @PutMapping
  public ApiResponse<StudentProfileResponse> updateProfile(
      @Valid @RequestBody StudentProfileUpdateRequest request, Principal principal) {
    return new ApiResponse<>(
        200, "Cập nhật hồ sơ học viên thành công", userService.updateStudentProfile(principal, request));
  }
}
