package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.request.AssignTeacherRequest;
import com.ntt.language_center_management.dto.request.ChangeClassStatusRequest;
import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.service.CourseClassService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/classes")
public class AdminCourseClassApiController {

  private final CourseClassService courseClassService;

  public AdminCourseClassApiController(CourseClassService courseClassService) {
    this.courseClassService = courseClassService;
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CourseClassResponse>> create(
      @Valid @RequestBody CourseClassRequest request) {
    request.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(201, "Tạo lớp học thành công", courseClassService.create(request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<CourseClassResponse> update(
      @PathVariable Integer id, @Valid @RequestBody CourseClassRequest request) {
    request.setId(id);
    return new ApiResponse<>(
        200, "Cập nhật lớp học thành công", courseClassService.update(id, request));
  }

  @PatchMapping("/{id}/teacher")
  public ApiResponse<CourseClassResponse> assignTeacher(
      @PathVariable Integer id, @Valid @RequestBody AssignTeacherRequest request) {
    return new ApiResponse<>(
        200,
        "Phân công giảng viên thành công",
        courseClassService.assignTeacher(id, request.teacherId()));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<CourseClassResponse> changeStatus(
      @PathVariable Integer id, @Valid @RequestBody ChangeClassStatusRequest request) {
    return new ApiResponse<>(
        200,
        "Đổi trạng thái lớp thành công",
        courseClassService.changeStatus(id, request.status()));
  }
}
