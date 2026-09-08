package com.ntt.language_center_management.controller.admin;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.DashboardSummaryResponse;
import com.ntt.language_center_management.dto.response.EnrollmentReportResponse;
import com.ntt.language_center_management.dto.response.PopularCourseReportResponse;
import com.ntt.language_center_management.dto.response.RevenueReportResponse;
import com.ntt.language_center_management.dto.response.TeacherLoadReportResponse;
import com.ntt.language_center_management.dto.response.UpcomingClassReportResponse;
import com.ntt.language_center_management.service.DashboardService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminDashboardApiController {
  private final DashboardService dashboardService;

  public AdminDashboardApiController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/dashboard/summary")
  public ApiResponse<DashboardSummaryResponse> summary() {
    return new ApiResponse<>(200, "Lấy tổng quan quản trị thành công", dashboardService.getSummary());
  }

  @GetMapping("/reports/revenue")
  public ApiResponse<List<RevenueReportResponse>> revenue(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return new ApiResponse<>(200, "Lấy báo cáo doanh thu thành công", dashboardService.getRevenue(from, to));
  }

  @GetMapping("/reports/enrollments")
  public ApiResponse<List<EnrollmentReportResponse>> enrollments(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return new ApiResponse<>(200, "Lấy báo cáo đăng ký thành công", dashboardService.getEnrollments(from, to));
  }

  @GetMapping("/reports/popular-courses")
  public ApiResponse<List<PopularCourseReportResponse>> popularCourses(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(defaultValue = "5") int limit) {
    return new ApiResponse<>(200, "Lấy báo cáo khóa học phổ biến thành công",
        dashboardService.getPopularCourses(from, to, limit));
  }

  @GetMapping("/reports/teacher-load")
  public ApiResponse<List<TeacherLoadReportResponse>> teacherLoad(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return new ApiResponse<>(200, "Lấy tải giảng dạy thành công", dashboardService.getTeacherLoad(from, to));
  }

  @GetMapping("/reports/upcoming-classes")
  public ApiResponse<List<UpcomingClassReportResponse>> upcomingClasses(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return new ApiResponse<>(200, "Lấy lớp sắp khai giảng thành công", dashboardService.getUpcomingClasses(from, to));
  }
}
