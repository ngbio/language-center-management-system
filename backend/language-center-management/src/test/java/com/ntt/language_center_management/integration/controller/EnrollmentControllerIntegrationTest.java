package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.management.StaffEnrollmentApiController;
import com.ntt.language_center_management.controller.student.EnrollmentApiController;
import com.ntt.language_center_management.dto.request.CancelEnrollmentRequest;
import com.ntt.language_center_management.dto.request.CreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.StaffCreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.TransferEnrollmentRequest;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.service.EnrollmentService;
import com.ntt.language_center_management.service.SystemLogService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({EnrollmentApiController.class, StaffEnrollmentApiController.class})
@Import(SecurityConfig.class)
class EnrollmentControllerIntegrationTest {
  @Autowired MockMvc mockMvc;
  @MockitoBean EnrollmentService enrollmentService;
  @MockitoBean UserService userService;
  @MockitoBean JwtUtils jwtUtils;
  @MockitoBean SystemLogService systemLogService;

  @Test @WithMockUser(roles = "STUDENT")
  void studentCanEnrollAndOnlyReadOwnEnrollmentData() throws Exception {
    mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
        .content("{\"courseClassId\":1}")).andExpect(status().isCreated());
    mockMvc.perform(get("/api/students/me/enrollments")).andExpect(status().isOk());
    mockMvc.perform(get("/api/students/me/courses")).andExpect(status().isOk());
    mockMvc.perform(get("/api/students/me/classes")).andExpect(status().isOk());
    mockMvc.perform(get("/api/students/me/schedules")).andExpect(status().isOk());
    verify(enrollmentService).enrollMe(any(CreateEnrollmentRequest.class), any(Principal.class));
  }

  @Test @WithMockUser(roles = "STUDENT")
  void duplicateFullCancelledStartedInactiveAndScheduleConflictsAreMapped() throws Exception {
    when(enrollmentService.enrollMe(any(CreateEnrollmentRequest.class), any()))
        .thenThrow(new DuplicateResourceException("Đã đăng ký"))
        .thenThrow(new IllegalArgumentException("Lớp không còn chỗ"))
        .thenThrow(new IllegalArgumentException("Lớp không mở đăng ký"))
        .thenThrow(new IllegalArgumentException("Tài khoản không ACTIVE"))
        .thenThrow(new IllegalArgumentException("Trùng lịch"));
    int[] expected = {409, 400, 400, 400, 400};
    for (int statusCode : expected) {
      mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
          .content("{\"courseClassId\":1}")).andExpect(status().is(statusCode));
    }
  }

  @Test @WithMockUser(roles = "CONSULTANT")
  void consultantCanCreateEnrollmentByStudentEmail() throws Exception {
    mockMvc.perform(post("/api/staff/enrollments").contentType(MediaType.APPLICATION_JSON)
        .content("{\"courseClassId\":1,\"studentEmail\":\"student@example.com\"}"))
        .andExpect(status().isCreated());
    verify(enrollmentService).enrollByStaff(any(StaffCreateEnrollmentRequest.class));
  }

  @Test @WithMockUser(roles = "ADMIN")
  void staffEnrollmentRejectsMissingOrNonStudentEmail() throws Exception {
    when(enrollmentService.enrollByStaff(any(StaffCreateEnrollmentRequest.class)))
        .thenThrow(new ResourceNotFoundException("Không tìm thấy Student"));
    mockMvc.perform(post("/api/staff/enrollments").contentType(MediaType.APPLICATION_JSON)
        .content("{\"courseClassId\":1,\"studentEmail\":\"missing@example.com\"}"))
        .andExpect(status().isNotFound());
    mockMvc.perform(post("/api/staff/enrollments").contentType(MediaType.APPLICATION_JSON)
        .content("{\"courseClassId\":1,\"studentEmail\":\"invalid\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test @WithMockUser(roles = "STUDENT")
  void cancellationRequiresOwnershipAndBusinessPolicy() throws Exception {
    when(enrollmentService.requestCancel(eq(1), any(CancelEnrollmentRequest.class), any()))
        .thenThrow(new ForbiddenException("Không phải chủ sở hữu"));
    mockMvc.perform(post("/api/enrollments/1/cancel-request").contentType(MediaType.APPLICATION_JSON)
        .content("{\"cancellationReason\":\"Không thể tham gia\"}"))
        .andExpect(status().isForbidden());
    when(enrollmentService.requestCancel(eq(2), any(CancelEnrollmentRequest.class), any()))
        .thenThrow(new IllegalArgumentException("Quá hạn hủy"));
    mockMvc.perform(post("/api/enrollments/2/cancel-request").contentType(MediaType.APPLICATION_JSON)
        .content("{\"cancellationReason\":\"Không thể tham gia\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test @WithMockUser(roles = "CONSULTANT")
  void staffStatusOnlyAllowsBusinessTransitions() throws Exception {
    mockMvc.perform(patch("/api/staff/enrollments/1/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"CANCELLED\"}")).andExpect(status().isOk());
    verify(enrollmentService).changeStatus(1, EnrollmentStatus.CANCELLED);
    when(enrollmentService.changeStatus(2, EnrollmentStatus.CONFIRMED))
        .thenThrow(new IllegalArgumentException("Transition không hợp lệ"));
    mockMvc.perform(patch("/api/staff/enrollments/2/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"CONFIRMED\"}")).andExpect(status().isBadRequest());
  }

  @Test @WithMockUser(roles = "ADMIN")
  void transferValidatesCourseCapacityConflictAndLockingServiceBoundary() throws Exception {
    when(enrollmentService.transfer(eq(1), any(TransferEnrollmentRequest.class)))
        .thenThrow(new IllegalArgumentException("Khác khóa học hoặc hết chỗ"));
    mockMvc.perform(post("/api/staff/enrollments/1/transfer").contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetCourseClassId\":2}")).andExpect(status().isBadRequest());
    when(enrollmentService.transfer(eq(2), any(TransferEnrollmentRequest.class)))
        .thenThrow(new IllegalArgumentException("Trùng lịch"));
    mockMvc.perform(post("/api/staff/enrollments/2/transfer").contentType(MediaType.APPLICATION_JSON)
        .content("{\"targetCourseClassId\":3}")).andExpect(status().isBadRequest());
  }

  @Test
  void enrollmentEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(post("/api/enrollments").contentType(MediaType.APPLICATION_JSON)
        .content("{\"courseClassId\":1}")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/staff/enrollments")).andExpect(status().isUnauthorized());
  }
}
