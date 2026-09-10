package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.student.StudentProfileApiController;
import com.ntt.language_center_management.controller.teacher.TeacherProfileApiController;
import com.ntt.language_center_management.dto.request.StudentProfileUpdateRequest;
import com.ntt.language_center_management.dto.request.TeacherProfileUpdateRequest;
import com.ntt.language_center_management.dto.response.StudentProfileResponse;
import com.ntt.language_center_management.dto.response.TeacherProfileResponse;
import com.ntt.language_center_management.service.TeacherService;
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

@WebMvcTest({StudentProfileApiController.class, TeacherProfileApiController.class})
@Import(SecurityConfig.class)
class ProfileControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private UserService userService;
  @MockitoBean private TeacherService teacherService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentCanReadOwnProfile() throws Exception {
    when(userService.getStudentProfile(any(Principal.class))).thenReturn(studentProfile());

    mockMvc.perform(get("/api/students/me/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentCode").value("HV000001"))
        .andExpect(jsonPath("$.data.email").value("student@example.com"));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentCanUpdateOwnProfile() throws Exception {
    when(userService.updateStudentProfile(any(Principal.class), any(StudentProfileUpdateRequest.class)))
        .thenReturn(studentProfile());

    mockMvc.perform(putJson("/api/students/me/profile", """
        {
          "fullName":"Student Updated","phoneNumber":"0901234567",
          "address":"Ho Chi Minh City","dateOfBirth":"2000-01-01",
          "gender":"FEMALE","avatar":"https://example.com/avatar.jpg"
        }
        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(userService).updateStudentProfile(any(Principal.class), any(StudentProfileUpdateRequest.class));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentUpdateRejectsBlankNameAndFutureBirthDate() throws Exception {
    mockMvc.perform(putJson("/api/students/me/profile", """
        {"fullName":" ","dateOfBirth":"2999-01-01","gender":"MALE"}
        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void studentProfileRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/students/me/profile"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void teacherCannotReadStudentProfile() throws Exception {
    mockMvc.perform(get("/api/students/me/profile"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void teacherCanReadOwnProfile() throws Exception {
    when(teacherService.getProfile(any(Principal.class))).thenReturn(teacherProfile());

    mockMvc.perform(get("/api/teachers/me/profile"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.teacherCode").value("GV000001"))
        .andExpect(jsonPath("$.data.email").value("teacher@example.com"));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void teacherCanUpdateOwnProfile() throws Exception {
    when(teacherService.updateProfile(any(Principal.class), any(TeacherProfileUpdateRequest.class)))
        .thenReturn(teacherProfile());

    mockMvc.perform(putJson("/api/teachers/me/profile", """
        {
          "fullName":"Teacher Updated","phoneNumber":"0901234567",
          "address":"Ho Chi Minh City","specialization":"English",
          "degree":"Master","experienceYears":8
        }
        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(teacherService).updateProfile(any(Principal.class), any(TeacherProfileUpdateRequest.class));
  }

  @Test
  @WithMockUser(username = "teacher@example.com", roles = "TEACHER")
  void teacherUpdateRejectsBlankNameAndInvalidExperience() throws Exception {
    mockMvc.perform(putJson("/api/teachers/me/profile", """
        {"fullName":" ","experienceYears":81}
        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void teacherProfileRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/api/teachers/me/profile"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void studentCannotReadTeacherProfile() throws Exception {
    mockMvc.perform(get("/api/teachers/me/profile"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder putJson(
      String path, String json) {
    return put(path).contentType(MediaType.APPLICATION_JSON).content(json);
  }

  private StudentProfileResponse studentProfile() {
    return new StudentProfileResponse(
        1, "HV000001", 10, "student", "Student One", "student@example.com",
        "0901234567", "Ho Chi Minh City", null, "FEMALE", null, "ACTIVE", null, null);
  }

  private TeacherProfileResponse teacherProfile() {
    return new TeacherProfileResponse(
        2, "GV000001", 20, "teacher", "Teacher One", "teacher@example.com",
        "0901234567", "Ho Chi Minh City", "English", "Master", 8, "ACTIVE", null, null);
  }
}
