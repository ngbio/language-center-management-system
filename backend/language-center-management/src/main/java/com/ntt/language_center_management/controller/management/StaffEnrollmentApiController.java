package com.ntt.language_center_management.controller.management;

import com.ntt.language_center_management.dto.request.ChangeEnrollmentStatusRequest;
import com.ntt.language_center_management.dto.request.StaffCreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.TransferEnrollmentRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
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
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api")
public class StaffEnrollmentApiController {

  private final EnrollmentService enrollmentService;

  public StaffEnrollmentApiController(EnrollmentService enrollmentService) {
    this.enrollmentService = enrollmentService;
  }

  @GetMapping("/staff/enrollments")
  public ApiResponse<PageResponse<EnrollmentResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer courseId,
      @RequestParam(required = false) Integer classId,
      @RequestParam(required = false) String enrollmentStatus,
      @RequestParam(required = false) String paymentStatus,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "enrollmentDate") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return new ApiResponse<>(200, "Lấy danh sách đăng ký thành công",
        enrollmentService.searchStaffEnrollments(keyword, courseId, classId, enrollmentStatus,
            paymentStatus, page, size, sort, direction));
  }

  @GetMapping("/staff/enrollments/{id}")
  public ApiResponse<EnrollmentResponse> getById(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy chi tiết đăng ký thành công",
        enrollmentService.getStaffEnrollment(id));
  }

  @PostMapping("/staff/enrollments")
  public ResponseEntity<ApiResponse<EnrollmentResponse>> enrollByStaff(
      @Valid @RequestBody StaffCreateEnrollmentRequest request) {
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
