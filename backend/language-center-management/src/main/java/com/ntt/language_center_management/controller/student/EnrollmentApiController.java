package com.ntt.language_center_management.controller.student;

import com.ntt.language_center_management.dto.request.CancelEnrollmentRequest;
import com.ntt.language_center_management.dto.request.CreateEnrollmentRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.service.EnrollmentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EnrollmentApiController {

  private final EnrollmentService enrollmentService;

  public EnrollmentApiController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  @PostMapping("/enrollments")
  public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollMe(
      @Valid @RequestBody CreateEnrollmentRequest request, Principal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new ApiResponse<>(
                201, "Đăng ký lớp học thành công", enrollmentService.enrollMe(request, principal)));
  }

  @GetMapping("/students/me/enrollments")
  public ApiResponse<List<EnrollmentSummaryResponse>> getMyEnrollments(Principal principal) {
    return new ApiResponse<>(
        200, "Lấy lịch sử đăng ký thành công", enrollmentService.getMyEnrollments(principal));
  }

  @GetMapping("/students/me/courses")
  public ApiResponse<List<CourseResponse>> getMyCourses(Principal principal) {
    return new ApiResponse<>(
        200, "Lấy danh sách khóa học của tôi thành công", enrollmentService.getMyCourses(principal));
  }

  @PostMapping("/enrollments/{id}/cancel-request")
  public ApiResponse<EnrollmentResponse> requestCancel(
      @PathVariable Integer id,
      @Valid @RequestBody CancelEnrollmentRequest request,
      Principal principal) {
    return new ApiResponse<>(
        200,
        "Hủy đăng ký thành công",
        enrollmentService.requestCancel(id, request, principal));
  }
}
