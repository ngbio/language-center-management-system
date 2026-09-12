package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.admin.AdminDashboardApiController;
import com.ntt.language_center_management.controller.media.ImageUploadApiController;
import com.ntt.language_center_management.dto.response.ImageUploadResponse;
import com.ntt.language_center_management.dto.response.DashboardSummaryResponse;
import com.ntt.language_center_management.dto.response.EnrollmentReportResponse;
import com.ntt.language_center_management.dto.response.PopularCourseReportResponse;
import com.ntt.language_center_management.dto.response.RevenueReportResponse;
import com.ntt.language_center_management.dto.response.TeacherLoadReportResponse;
import com.ntt.language_center_management.dto.response.UpcomingClassReportResponse;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.service.DashboardService;
import com.ntt.language_center_management.service.ImageUploadService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.security.Principal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(
    controllers = {AdminDashboardApiController.class, ImageUploadApiController.class},
    properties = "app.cors.allowed-origins=http://localhost:5173")
@Import(SecurityConfig.class)
class DashboardUploadSecurityIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private DashboardService dashboardService;
  @MockitoBean private ImageUploadService imageUploadService;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminAccessesEveryDashboardReport() throws Exception {
    LocalDate from = LocalDate.of(2026, 9, 1);
    LocalDate to = LocalDate.of(2026, 9, 30);
    when(dashboardService.getRevenue(from, to)).thenReturn(List.of());
    when(dashboardService.getEnrollments(from, to)).thenReturn(List.of());
    when(dashboardService.getPopularCourses(from, to, 5)).thenReturn(List.of());
    when(dashboardService.getTeacherLoad(from, to)).thenReturn(List.of());
    when(dashboardService.getUpcomingClasses(from, to)).thenReturn(List.of());

    mockMvc.perform(get("/api/admin/dashboard/summary")).andExpect(status().isOk());
    mockMvc.perform(report("/api/admin/reports/revenue")).andExpect(status().isOk());
    mockMvc.perform(report("/api/admin/reports/enrollments")).andExpect(status().isOk());
    mockMvc.perform(report("/api/admin/reports/popular-courses")).andExpect(status().isOk());
    mockMvc.perform(report("/api/admin/reports/teacher-load")).andExpect(status().isOk());
    mockMvc.perform(report("/api/admin/reports/upcoming-classes")).andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void dashboardSerializesSummaryAndEveryCalculatedReportShape() throws Exception {
    LocalDate from = LocalDate.of(2026, 9, 1);
    LocalDate to = LocalDate.of(2026, 9, 30);
    when(dashboardService.getSummary()).thenReturn(new DashboardSummaryResponse(
        100, 8, 12, 5, 3, 4, 80, new BigDecimal("50000000"),
        new BigDecimal("2000000"), new BigDecimal("48000000")));
    when(dashboardService.getRevenue(from, to)).thenReturn(List.of(
        new RevenueReportResponse(YearMonth.of(2026, 9), new BigDecimal("50000000"),
            new BigDecimal("2000000"), new BigDecimal("48000000"))));
    when(dashboardService.getEnrollments(from, to)).thenReturn(List.of(
        new EnrollmentReportResponse(YearMonth.of(2026, 9), 10, 8, 7, 2)));
    when(dashboardService.getPopularCourses(from, to, 5)).thenReturn(List.of(
        new PopularCourseReportResponse(1, "EN-A1", "English A1", 10, 7,
            new BigDecimal("21000000"))));
    when(dashboardService.getTeacherLoad(from, to)).thenReturn(List.of(
        new TeacherLoadReportResponse(2, "GV002", "Teacher Two", 2, 12, 8)));
    when(dashboardService.getUpcomingClasses(from, to)).thenReturn(List.of(
        new UpcomingClassReportResponse(5, "EN-A1-01", "English Morning", 1,
            "English A1", "Teacher Two", LocalDate.of(2026, 9, 15),
            LocalDate.of(2026, 11, 15), 20, 12, 8, new BigDecimal("3000000"), "OPEN")));

    mockMvc.perform(get("/api/admin/dashboard/summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalStudents").value(100))
        .andExpect(jsonPath("$.data.netRevenue").value(48000000));
    mockMvc.perform(report("/api/admin/reports/revenue"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].grossRevenue").value(50000000))
        .andExpect(jsonPath("$.data[0].refundedAmount").value(2000000));
    mockMvc.perform(report("/api/admin/reports/enrollments"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].confirmed").value(8))
        .andExpect(jsonPath("$.data[0].cancelled").value(2));
    mockMvc.perform(report("/api/admin/reports/popular-courses"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].paidEnrollments").value(7));
    mockMvc.perform(report("/api/admin/reports/teacher-load"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].lessons").value(12))
        .andExpect(jsonPath("$.data[0].completedLessons").value(8));
    mockMvc.perform(report("/api/admin/reports/upcoming-classes"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].availableSeats").value(8));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void reportValidatesDateRangeAndForwardsPopularCourseLimit() throws Exception {
    LocalDate from = LocalDate.of(2026, 9, 1);
    LocalDate to = LocalDate.of(2026, 9, 30);
    when(dashboardService.getRevenue(to, from))
        .thenThrow(new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc"));
    when(dashboardService.getPopularCourses(from, to, 3)).thenReturn(List.of());

    mockMvc.perform(get("/api/admin/reports/revenue")
            .param("from", "2026-09-30").param("to", "2026-09-01"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
    mockMvc.perform(get("/api/admin/reports/popular-courses")
            .param("from", "2026-09-01").param("to", "2026-09-30").param("limit", "3"))
        .andExpect(status().isOk());

    verify(dashboardService).getPopularCourses(from, to, 3);
  }

  @Test
  @WithMockUser(roles = "CONSULTANT")
  void consultantCannotAccessAdminDashboardReports() throws Exception {
    mockMvc.perform(report("/api/admin/reports/upcoming-classes"))
        .andExpect(status().isForbidden());
    mockMvc.perform(report("/api/admin/reports/revenue"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/admin/dashboard/summary"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void reportRejectsMalformedDate() throws Exception {
    mockMvc.perform(get("/api/admin/reports/revenue")
            .param("from", "not-a-date").param("to", "2026-09-30"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void dashboardRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/admin/dashboard/summary"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void malformedOrExpiredBearerTokenReturnsConsistentJson401() throws Exception {
    when(jwtUtils.validateTokenAndGetUsername("expired-token"))
        .thenThrow(new IllegalArgumentException("expired"));

    mockMvc.perform(get("/api/admin/dashboard/summary")
            .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value(
            "Token không hợp lệ, đã hết hạn hoặc tài khoản không khả dụng"));
    mockMvc.perform(get("/api/admin/dashboard/summary")
            .header(HttpHeaders.AUTHORIZATION, "Token invalid"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void adminDoesNotAutomaticallyAccessStudentOrTeacherOnlyEndpoints() throws Exception {
    mockMvc.perform(get("/api/students/me/attendance"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
    mockMvc.perform(get("/api/teachers/me/profile"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @WithMockUser(username = "admin@example.com", roles = "ADMIN")
  void adminUploadsCourseImage() throws Exception {
    MockMultipartFile image = image("course.png", "image/png");
    when(imageUploadService.upload(any(), eq("COURSE"), any(Principal.class)))
        .thenReturn(new ImageUploadResponse(
            "https://cdn.example/course.png", "courses/course", "png", 1200, 630, 2048));

    mockMvc.perform(multipart("/api/uploads/images")
            .file(image).param("purpose", "COURSE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.publicId").value("courses/course"))
        .andExpect(jsonPath("$.data.width").value(1200));

    verify(imageUploadService).upload(any(), eq("COURSE"),
        argThat(principal -> principal.getName().equals("admin@example.com")));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentUploadsAvatar() throws Exception {
    when(imageUploadService.upload(any(), eq("AVATAR"), any(Principal.class)))
        .thenReturn(new ImageUploadResponse(
            "https://cdn.example/avatar.jpg", "avatars/student", "jpg", 400, 400, 1024));

    mockMvc.perform(multipart("/api/uploads/images")
            .file(image("avatar.jpg", "image/jpeg")).param("purpose", "AVATAR"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.format").value("jpg"));
  }

  @Test
  @WithMockUser(roles = "TEACHER")
  void teacherCannotUploadThroughRestrictedEndpoint() throws Exception {
    mockMvc.perform(multipart("/api/uploads/images")
            .file(image("avatar.jpg", "image/jpeg")).param("purpose", "AVATAR"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "CONSULTANT")
  void consultantCannotUploadThroughRestrictedEndpoint() throws Exception {
    mockMvc.perform(multipart("/api/uploads/images")
            .file(image("course.png", "image/png")).param("purpose", "COURSE"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotUseCourseImagePurpose() throws Exception {
    when(imageUploadService.upload(any(), eq("COURSE"), any(Principal.class)))
        .thenThrow(new ForbiddenException("Student chỉ được tải ảnh đại diện"));

    mockMvc.perform(multipart("/api/uploads/images")
            .file(image("course.png", "image/png")).param("purpose", "COURSE"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void uploadMapsInvalidFileToBadRequest() throws Exception {
    when(imageUploadService.upload(any(), eq("AVATAR"), any(Principal.class)))
        .thenThrow(new IllegalArgumentException("Tệp tải lên phải là hình ảnh"));

    mockMvc.perform(multipart("/api/uploads/images")
            .file(image("notes.txt", MediaType.TEXT_PLAIN_VALUE)).param("purpose", "AVATAR"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void emptyAndOversizedUploadsReturnExpectedStatuses() throws Exception {
    when(imageUploadService.upload(any(), eq("AVATAR"), any(Principal.class)))
        .thenThrow(new IllegalArgumentException("Vui lòng chọn một ảnh"))
        .thenThrow(new MaxUploadSizeExceededException(5L * 1024 * 1024));

    mockMvc.perform(multipart("/api/uploads/images")
            .file(new MockMultipartFile("file", "empty.png", "image/png", new byte[0]))
            .param("purpose", "AVATAR"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));

    mockMvc.perform(multipart("/api/uploads/images")
            .file(image("large.png", "image/png")).param("purpose", "AVATAR"))
        .andExpect(status().isPayloadTooLarge())
        .andExpect(jsonPath("$.status").value(413));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void cloudinaryFailureDoesNotExposeCredentials() throws Exception {
    when(imageUploadService.upload(any(), eq("COURSE"), any(Principal.class)))
        .thenThrow(new RuntimeException("api_secret=must-not-leak"));

    mockMvc.perform(multipart("/api/uploads/images")
            .file(image("course.png", "image/png")).param("purpose", "COURSE"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.status").value(500))
        .andExpect(jsonPath("$.message").value(
            "Hệ thống gặp lỗi khi xử lý yêu cầu. Vui lòng thử lại hoặc kiểm tra log backend."));
  }

  @Test
  void corsPreflightAllowsConfiguredFrontendOrigin() throws Exception {
    mockMvc.perform(options("/api/uploads/images")
            .header(HttpHeaders.ORIGIN, "http://localhost:5173")
            .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
            "http://localhost:5173"));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder report(
      String path) {
    return get(path).param("from", "2026-09-01").param("to", "2026-09-30");
  }

  private MockMultipartFile image(String filename, String contentType) {
    return new MockMultipartFile("file", filename, contentType, new byte[] {1, 2, 3});
  }
}
