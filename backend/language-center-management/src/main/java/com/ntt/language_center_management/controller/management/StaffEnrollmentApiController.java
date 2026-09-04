package com.ntt.language_center_management.controller.management;

import com.ntt.language_center_management.dto.request.ChangeEnrollmentStatusRequest;
import com.ntt.language_center_management.dto.request.CreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.TransferEnrollmentRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.service.EnrollmentService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StaffEnrollmentApiController {

  private final EnrollmentService enrollmentService;

  public StaffEnrollmentApiController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  @PostMapping("/staff/enrollments")
  public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollByStaff(
      @Valid @RequestBody CreateEnrollmentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new ApiResponse<>(
                201, "Xếp lớp cho học viên thành công", enrollmentService.enrollByStaff(request)));
  }

  @GetMapping("/classes/{id}/enrollments")
  public ApiResponse<List<EnrollmentSummaryResponse>> getClassEnrollments(
      @PathVariable Integer id, Principal principal) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách đăng ký của lớp thành công",
        enrollmentService.getClassEnrollments(id, principal));
  }

  @PatchMapping("/staff/enrollments/{id}/status")
  public ApiResponse<EnrollmentResponse> changeStatus(
      @PathVariable Integer id, @Valid @RequestBody ChangeEnrollmentStatusRequest request) {
    return new ApiResponse<>(
        200,
        "Cập nhật trạng thái đăng ký thành công",
        enrollmentService.changeStatus(id, request.status()));
  }

  @PostMapping("/staff/enrollments/{id}/transfer")
  public ApiResponse<EnrollmentResponse> transfer(
      @PathVariable Integer id, @Valid @RequestBody TransferEnrollmentRequest request) {
    return new ApiResponse<>(
        200, "Chuyển lớp thành công", enrollmentService.transfer(id, request));
  }
}
