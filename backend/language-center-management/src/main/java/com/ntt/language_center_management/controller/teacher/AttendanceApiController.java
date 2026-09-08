package com.ntt.language_center_management.controller.teacher;

import com.ntt.language_center_management.dto.request.AttendanceBulkRequest;
import com.ntt.language_center_management.dto.request.AttendanceUpdateRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.AttendanceResponse;
import com.ntt.language_center_management.dto.response.AttendanceSheetResponse;
import com.ntt.language_center_management.dto.response.ClassAttendanceSummaryResponse;
import com.ntt.language_center_management.service.AttendanceService;
import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AttendanceApiController {
  private final AttendanceService attendanceService;

  public AttendanceApiController(AttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @GetMapping("/lessons/{lessonId}/attendance")
  public ApiResponse<AttendanceSheetResponse> getSheet(
      @PathVariable Integer lessonId, Principal principal) {
    return new ApiResponse<>(
        200, "Lấy bảng điểm danh thành công", attendanceService.getSheet(lessonId, principal));
  }

  @PutMapping("/lessons/{lessonId}/attendance")
  public ApiResponse<AttendanceSheetResponse> saveBulk(
      @PathVariable Integer lessonId,
      @Valid @RequestBody AttendanceBulkRequest request,
      Principal principal) {
    return new ApiResponse<>(
        200,
        "Lưu bảng điểm danh thành công",
        attendanceService.saveBulk(lessonId, request, principal));
  }

  @PatchMapping("/attendance/{attendanceId}")
  public ApiResponse<AttendanceResponse> update(
      @PathVariable Integer attendanceId,
      @Valid @RequestBody AttendanceUpdateRequest request,
      Principal principal) {
    return new ApiResponse<>(
        200,
        "Cập nhật điểm danh thành công",
        attendanceService.update(attendanceId, request, principal));
  }

  @GetMapping("/classes/{classId}/attendance-summary")
  public ApiResponse<ClassAttendanceSummaryResponse> getClassSummary(
      @PathVariable Integer classId, Principal principal) {
    return new ApiResponse<>(
        200,
        "Lấy tổng hợp điểm danh thành công",
        attendanceService.getClassSummary(classId, principal));
  }
}
