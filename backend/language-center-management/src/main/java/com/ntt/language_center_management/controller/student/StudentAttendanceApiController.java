package com.ntt.language_center_management.controller.student;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.AttendanceResponse;
import com.ntt.language_center_management.service.AttendanceService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/me")
public class StudentAttendanceApiController {
  private final AttendanceService attendanceService;

  public StudentAttendanceApiController(AttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @GetMapping("/attendance")
  public ApiResponse<List<AttendanceResponse>> getMine(Principal principal) {
    return new ApiResponse<>(
        200, "Lấy lịch sử điểm danh thành công", attendanceService.getMine(principal));
  }
}
