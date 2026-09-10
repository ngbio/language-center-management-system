package com.ntt.language_center_management.integration.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.admin.AdminAuthApiController;
import com.ntt.language_center_management.controller.management.StaffAuthApiController;
import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminAuthApiController.class, StaffAuthApiController.class})
@Import(SecurityConfig.class)
class StaffAdminAuthControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  void adminLoginReturnsTokenForAdminAccount() throws Exception {
    LoginRequest request = new LoginRequest("admin@example.com", "Admin@123");
    when(userService.loginAdmin(request)).thenReturn(user("ADMIN"));
    when(jwtUtils.generateToken("admin@example.com")).thenReturn("admin-token");

    mockMvc.perform(login("/api/admin/auth/login", "admin@example.com", "Admin@123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").value("admin-token"))
        .andExpect(jsonPath("$.data.roleCode").value("ADMIN"));
  }

  @Test
  void staffLoginReturnsTokenForConsultantAccount() throws Exception {
    LoginRequest request = new LoginRequest("staff@example.com", "Staff@123");
    when(userService.loginStaff(request)).thenReturn(user("CONSULTANT"));
    when(jwtUtils.generateToken("staff@example.com")).thenReturn("staff-token");

    mockMvc.perform(login("/api/staff/auth/login", "staff@example.com", "Staff@123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").value("staff-token"))
        .andExpect(jsonPath("$.data.roleCode").value("CONSULTANT"));
  }

  @Test
  void adminLoginMapsWrongRoleToUnauthorized() throws Exception {
    LoginRequest request = new LoginRequest("student@example.com", "Student@123");
    when(userService.loginAdmin(request)).thenThrow(new UnauthorizedException("Sai cổng đăng nhập"));

    mockMvc.perform(login("/api/admin/auth/login", "student@example.com", "Student@123"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void staffLoginRejectsInvalidEmailBeforeCallingService() throws Exception {
    mockMvc.perform(login("/api/staff/auth/login", "invalid", "Staff@123"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder login(
      String path, String email, String password) {
    return post(path)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
  }

  private UserResponse user(String roleCode) {
    String email = roleCode.equals("ADMIN") ? "admin@example.com" : "staff@example.com";
    return new UserResponse(
        1, roleCode.toLowerCase(), roleCode, email, null, null, roleCode, roleCode,
        AccountStatus.ACTIVE, null, null);
  }
}
