package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.student.StudentAttendanceApiController;
import com.ntt.language_center_management.controller.teacher.AttendanceApiController;
import com.ntt.language_center_management.dto.request.AttendanceBulkRequest;
import com.ntt.language_center_management.dto.request.AttendanceUpdateRequest;
import com.ntt.language_center_management.dto.response.AttendanceResponse;
import com.ntt.language_center_management.dto.response.AttendanceSheetResponse;
import com.ntt.language_center_management.dto.response.AttendanceSheetItemResponse;
import com.ntt.language_center_management.dto.response.ClassAttendanceSummaryResponse;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.service.AttendanceService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AttendanceApiController.class, StudentAttendanceApiController.class})
@Import(SecurityConfig.class)
class AttendanceControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private AttendanceService attendanceService;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void teacherGetsAttendanceSheetForAssignedLesson() throws Exception {
    when(attendanceService.getSheet(eq(20), any(Principal.class)))
        .thenReturn(new AttendanceSheetResponse(
            20, new Date(), "Unit 1", "SCHEDULED", 5, "EN-A1", "English A1", List.of()));

    mockMvc.perform(get("/api/lessons/20/attendance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lessonId").value(20))
        .andExpect(jsonPath("$.data.classCode").value("EN-A1"));

    verify(attendanceService).getSheet(eq(20),
        argThat(principal -> principal.getName().equals("teacher@example.com")));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void attendanceSheetSerializesOnlyEligiblePaidStudentsReturnedByService() throws Exception {
    when(attendanceService.getSheet(eq(20), any(Principal.class)))
        .thenReturn(new AttendanceSheetResponse(20, new Date(), "Unit 1", "SCHEDULED",
            5, "EN-A1", "English A1", List.of(
                new AttendanceSheetItemResponse(null, 7, "ST001", "Paid Student",
                    null, null, null))));

    mockMvc.perform(get("/api/lessons/20/attendance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.students.length()").value(1))
        .andExpect(jsonPath("$.data.students[0].studentCode").value("ST001"));
  }

  @Test
  @WithMockUser(username = "teacher2@example.com", roles = "TEACHER")
  void teacherCannotReadAnotherTeachersLesson() throws Exception {
    when(attendanceService.getSheet(eq(20), any(Principal.class)))
        .thenThrow(new ForbiddenException("Không phụ trách lớp học này"));

    mockMvc.perform(get("/api/lessons/20/attendance"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void teacherSavesBulkAttendance() throws Exception {
    when(attendanceService.saveBulk(eq(20), any(AttendanceBulkRequest.class), any(Principal.class)))
        .thenReturn(new AttendanceSheetResponse(
            20, new Date(), "Unit 1", "COMPLETED", 5, "EN-A1", "English A1", List.of()));

    mockMvc.perform(put("/api/lessons/20/attendance")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"attendances":[
                  {"studentId":7,"status":"PRESENT","note":"Đúng giờ"},
                  {"studentId":8,"status":"ABSENT","note":"Có phép"}
                ]}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lessonStatus").value("COMPLETED"));

    verify(attendanceService).saveBulk(eq(20),
        argThat(request -> request.attendances().size() == 2), any(Principal.class));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void cancelledLessonAndExpiredAttendanceWindowReturnBadRequest() throws Exception {
    when(attendanceService.saveBulk(eq(20), any(AttendanceBulkRequest.class),
        any(Principal.class)))
        .thenThrow(new IllegalArgumentException("Không thể điểm danh buổi học đã hủy"));
    when(attendanceService.update(eq(40), any(AttendanceUpdateRequest.class),
        any(Principal.class)))
        .thenThrow(new IllegalArgumentException(
            "Chỉ được cập nhật điểm danh trong vòng 7 ngày sau buổi học"));

    mockMvc.perform(put("/api/lessons/20/attendance")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"attendances\":[{\"studentId\":7,\"status\":\"PRESENT\"}]}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Không thể điểm danh buổi học đã hủy"));
    mockMvc.perform(patch("/api/attendance/40")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"PRESENT\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value(
            "Chỉ được cập nhật điểm danh trong vòng 7 ngày sau buổi học"));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void completedLessonStillAcceptsAttendanceWithinAllowedWindow() throws Exception {
    when(attendanceService.saveBulk(eq(20), any(AttendanceBulkRequest.class),
        any(Principal.class))).thenReturn(new AttendanceSheetResponse(
            20, new Date(), "Unit 1", "COMPLETED", 5, "EN-A1", "English A1", List.of()));

    mockMvc.perform(put("/api/lessons/20/attendance")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"attendances\":[{\"studentId\":7,\"status\":\"PRESENT\"}]}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.lessonStatus").value("COMPLETED"));
  }

  @Test
  @WithMockUser(roles = "TEACHER")
  void bulkAttendanceRejectsEmptyList() throws Exception {
    mockMvc.perform(put("/api/lessons/20/attendance")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"attendances\":[]}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "TEACHER")
  void singleAttendanceUpdateValidatesStatus() throws Exception {
    mockMvc.perform(patch("/api/attendance/40")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"note\":\"Thiếu trạng thái\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void teacherUpdatesSingleAttendance() throws Exception {
    when(attendanceService.update(eq(40), any(AttendanceUpdateRequest.class), any(Principal.class)))
        .thenReturn(attendance());

    mockMvc.perform(patch("/api/attendance/40")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"LATE\",\"note\":\"Trễ 10 phút\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("LATE"));
  }

  @Test
  @WithMockUser(username = "teacher2@example.com", roles = "TEACHER")
  void teacherCannotUpdateAttendanceFromAnotherTeachersClass() throws Exception {
    when(attendanceService.update(eq(40), any(AttendanceUpdateRequest.class),
        any(Principal.class))).thenThrow(new ForbiddenException("Không phụ trách lớp học này"));

    mockMvc.perform(patch("/api/attendance/40")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"status\":\"ABSENT\",\"note\":\"Có phép\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void assignedTeacherReadsClassAttendanceSummary() throws Exception {
    when(attendanceService.getClassSummary(eq(5), any(Principal.class)))
        .thenReturn(new ClassAttendanceSummaryResponse(
            5, "EN-A1", "English A1", 12, 8, List.of()));

    mockMvc.perform(get("/api/classes/5/attendance-summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.classId").value(5))
        .andExpect(jsonPath("$.data.totalLessons").value(12))
        .andExpect(jsonPath("$.data.completedLessons").value(8));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void attendanceSummaryHandlesClassWithoutCompletedLessons() throws Exception {
    when(attendanceService.getClassSummary(eq(5), any(Principal.class)))
        .thenReturn(new ClassAttendanceSummaryResponse(
            5, "EN-A1", "English A1", 0, 0, List.of()));

    mockMvc.perform(get("/api/classes/5/attendance-summary"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalLessons").value(0))
        .andExpect(jsonPath("$.data.completedLessons").value(0))
        .andExpect(jsonPath("$.data.students.length()").value(0));
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotUseTeacherAttendanceEndpoints() throws Exception {
    mockMvc.perform(get("/api/lessons/20/attendance"))
        .andExpect(status().isForbidden());
    mockMvc.perform(get("/api/classes/5/attendance-summary"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentReadsOnlyOwnAttendanceHistory() throws Exception {
    when(attendanceService.getMine(any(Principal.class))).thenReturn(List.of(attendance()));

    mockMvc.perform(get("/api/students/me/attendance"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].studentId").value(7))
        .andExpect(jsonPath("$.data[0].status").value("LATE"));

    verify(attendanceService).getMine(
        argThat(principal -> principal.getName().equals("student@example.com")));
  }

  @Test
  void attendanceEndpointsRequireAuthentication() throws Exception {
    mockMvc.perform(get("/api/students/me/attendance"))
        .andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/lessons/20/attendance"))
        .andExpect(status().isUnauthorized());
  }

  private AttendanceResponse attendance() {
    return new AttendanceResponse(40, 20, new Date(), "Unit 1", 5, "EN-A1",
        "English A1", 7, "ST001", "Student One", "LATE", "Trễ 10 phút", new Date());
  }
}
