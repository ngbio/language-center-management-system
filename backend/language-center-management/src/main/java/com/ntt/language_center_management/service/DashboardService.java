package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.DashboardSummaryResponse;
import com.ntt.language_center_management.dto.response.EnrollmentReportResponse;
import com.ntt.language_center_management.dto.response.PopularCourseReportResponse;
import com.ntt.language_center_management.dto.response.RevenueReportResponse;
import com.ntt.language_center_management.dto.response.TeacherLoadReportResponse;
import com.ntt.language_center_management.dto.response.UpcomingClassReportResponse;
import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
  DashboardSummaryResponse getSummary();

  List<RevenueReportResponse> getRevenue(LocalDate from, LocalDate to);

  List<EnrollmentReportResponse> getEnrollments(LocalDate from, LocalDate to);

  List<PopularCourseReportResponse> getPopularCourses(LocalDate from, LocalDate to, int limit);

  List<TeacherLoadReportResponse> getTeacherLoad(LocalDate from, LocalDate to);

  List<UpcomingClassReportResponse> getUpcomingClasses(LocalDate from, LocalDate to);
}
