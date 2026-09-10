package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.management.ClassScheduleLessonApiController;
import com.ntt.language_center_management.dto.request.LessonRescheduleRequest;
import com.ntt.language_center_management.dto.request.LessonUpdateRequest;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.service.ClassScheduleService;
import com.ntt.language_center_management.service.LessonService;
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

@WebMvcTest(ClassScheduleLessonApiController.class)
@Import(SecurityConfig.class)
class LessonControllerIntegrationTest {
  @Autowired MockMvc mockMvc;
  @MockitoBean LessonService lessonService;
  @MockitoBean ClassScheduleService classScheduleService;
  @MockitoBean UserService userService;
  @MockitoBean JwtUtils jwtUtils;

  @Test @WithMockUser(roles = "ADMIN")
  void adminCanGenerateAndReadLessons() throws Exception {
    mockMvc.perform(post("/api/classes/1/lessons/generate")).andExpect(status().isCreated());
    mockMvc.perform(get("/api/classes/1/lessons")).andExpect(status().isOk());
    verify(lessonService).generate(eq(1), any(Principal.class));
    verify(lessonService).getByClassId(eq(1), any(Principal.class));
  }

  @Test @WithMockUser(roles = "ADMIN")
  void generationDateDuplicateAndSessionLimitsAreMapped() throws Exception {
    when(lessonService.generate(eq(1), any())).thenThrow(new IllegalArgumentException("Không sinh trước khai giảng"));
    mockMvc.perform(post("/api/classes/1/lessons/generate")).andExpect(status().isBadRequest());
    when(lessonService.generate(eq(2), any())).thenThrow(new DuplicateResourceException("Lesson trùng"));
    mockMvc.perform(post("/api/classes/2/lessons/generate")).andExpect(status().isConflict());
    when(lessonService.generate(eq(3), any())).thenThrow(new IllegalArgumentException("Vượt totalSessions"));
    mockMvc.perform(post("/api/classes/3/lessons/generate")).andExpect(status().isBadRequest());
  }

  @Test @WithMockUser(roles = "TEACHER")
  void teacherOnlyWorksWithAssignedClasses() throws Exception {
    when(lessonService.getByClassId(eq(2), any())).thenThrow(new ForbiddenException("Không phụ trách lớp"));
    mockMvc.perform(get("/api/classes/2/lessons")).andExpect(status().isForbidden());
    when(lessonService.update(eq(9), any(LessonUpdateRequest.class), any()))
        .thenThrow(new ForbiddenException("Không phụ trách lớp"));
    mockMvc.perform(put("/api/lessons/9").contentType(MediaType.APPLICATION_JSON)
        .content("{\"topic\":\"Grammar\"}"))
        .andExpect(status().isForbidden());
  }

  @Test @WithMockUser(roles = "ADMIN")
  void lessonContentCanBeUpdatedAndInvalidPayloadIsRejected() throws Exception {
    mockMvc.perform(put("/api/lessons/1").contentType(MediaType.APPLICATION_JSON)
        .content("{\"topic\":\"Grammar\"}"))
        .andExpect(status().isOk());
    mockMvc.perform(put("/api/lessons/1").contentType(MediaType.APPLICATION_JSON)
        .content("{\"topic\":\"" + "x".repeat(256) + "\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test @WithMockUser(roles = "ADMIN")
  void rescheduleChecksAttendanceAndActualResourceConflicts() throws Exception {
    when(lessonService.reschedule(eq(1), any(LessonRescheduleRequest.class)))
        .thenThrow(new IllegalArgumentException("Lesson đã có điểm danh"));
    mockMvc.perform(patch("/api/lessons/1/reschedule").contentType(MediaType.APPLICATION_JSON)
        .content("{\"lessonDate\":\"2027-02-15\",\"reason\":\"Nghỉ lễ\"}"))
        .andExpect(status().isBadRequest());
    when(lessonService.reschedule(eq(2), any(LessonRescheduleRequest.class)))
        .thenThrow(new DuplicateResourceException("Trùng phòng hoặc Teacher"));
    mockMvc.perform(patch("/api/lessons/2/reschedule").contentType(MediaType.APPLICATION_JSON)
        .content("{\"lessonDate\":\"2027-02-16\",\"reason\":\"Đổi lịch\"}"))
        .andExpect(status().isConflict());
  }

  @Test @WithMockUser(roles = "CONSULTANT")
  void consultantCannotRescheduleOrCancelLesson() throws Exception {
    mockMvc.perform(patch("/api/lessons/1/reschedule").contentType(MediaType.APPLICATION_JSON)
        .content("{\"lessonDate\":\"2027-02-15\",\"reason\":\"Đổi lịch\"}"))
        .andExpect(status().isForbidden());
    mockMvc.perform(patch("/api/lessons/1/cancel")).andExpect(status().isForbidden());
  }

  @Test @WithMockUser(roles = "ADMIN")
  void adminCancellationRejectsInvalidLessonState() throws Exception {
    when(lessonService.cancel(1)).thenThrow(new IllegalArgumentException("Không thể hủy lesson"));
    mockMvc.perform(patch("/api/lessons/1/cancel")).andExpect(status().isBadRequest());
  }
}
