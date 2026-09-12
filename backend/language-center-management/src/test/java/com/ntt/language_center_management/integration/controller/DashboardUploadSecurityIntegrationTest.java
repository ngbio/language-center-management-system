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
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.service.DashboardService;
import com.ntt.language_center_management.service.ImageUploadService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.security.Principal;
import java.time.LocalDate;
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
