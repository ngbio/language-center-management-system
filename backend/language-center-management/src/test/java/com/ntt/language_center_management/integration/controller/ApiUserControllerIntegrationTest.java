package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.controller.publicapi.ApiUserController;
import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.dto.request.ChangePasswordRequest;
import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.request.TeacherRegisterRequest;
import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.service.PasswordResetService;
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

@WebMvcTest(ApiUserController.class)
@Import(SecurityConfig.class)
class ApiUserControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private UserService userService;

  @MockitoBean private JwtUtils jwtUtils;

  @MockitoBean private PasswordResetService passwordResetService;

  @Test
  void forgotPasswordReturnsGenericResponseWithoutAuthentication() throws Exception {
    mockMvc.perform(postJson("/api/auth/forgot-password", "{\"email\":\"student@example.com\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.message").isNotEmpty());

    verify(passwordResetService).requestReset("student@example.com");
  }

  @Test
  void forgotPasswordRejectsMalformedEmail() throws Exception {
    mockMvc.perform(postJson("/api/auth/forgot-password", "{\"email\":\"not-an-email\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void validateResetPasswordTokenReturnsOkForValidToken() throws Exception {
    when(passwordResetService.isTokenValid("valid-token")).thenReturn(true);

    mockMvc.perform(get("/api/auth/reset-password/validate").param("token", "valid-token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));
  }

  @Test
  void validateResetPasswordTokenRejectsExpiredToken() throws Exception {
    when(passwordResetService.isTokenValid("expired-token")).thenReturn(false);

    mockMvc.perform(get("/api/auth/reset-password/validate").param("token", "expired-token"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void resetPasswordReturnsOkForValidRequest() throws Exception {
    mockMvc.perform(postJson("/api/auth/reset-password", """
        {"token":"valid-token","newPassword":"New@1234","confirmPassword":"New@1234"}
        """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200));

    verify(passwordResetService).resetPassword(any());
  }

  @Test
  void resetPasswordRejectsWeakPassword() throws Exception {
    mockMvc.perform(postJson("/api/auth/reset-password", """
        {"token":"valid-token","newPassword":"password","confirmPassword":"password"}
        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void loginReturnsTokenAndRoleForValidCredentials() throws Exception {
    when(userService.login(new LoginRequest("student@example.com", "Student@123")))
        .thenReturn(user("STUDENT"));
    when(jwtUtils.generateToken("student@example.com")).thenReturn("jwt-token");

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"email":"student@example.com","password":"Student@123"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").value("jwt-token"))
        .andExpect(jsonPath("$.data.roleCode").value("STUDENT"));
  }

  @Test
  void loginReturnsTeacherRoleForActiveTeacher() throws Exception {
    LoginRequest request = new LoginRequest("teacher@example.com", "Teacher@123");
    when(userService.login(request)).thenReturn(user("teacher@example.com", "TEACHER", AccountStatus.ACTIVE));
    when(jwtUtils.generateToken("teacher@example.com")).thenReturn("teacher-token");

    mockMvc.perform(postJson("/api/auth/login",
            "{\"email\":\"teacher@example.com\",\"password\":\"Teacher@123\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").value("teacher-token"))
        .andExpect(jsonPath("$.data.roleCode").value("TEACHER"));
  }

  @Test
  void loginMapsInvalidCredentialsToUnauthorized() throws Exception {
    LoginRequest request = new LoginRequest("missing@example.com", "Wrong@123");
    when(userService.login(request)).thenThrow(new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

    mockMvc.perform(postJson("/api/auth/login",
            "{\"email\":\"missing@example.com\",\"password\":\"Wrong@123\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void loginMapsInactiveAccountToUnauthorized() throws Exception {
    LoginRequest request = new LoginRequest("inactive@example.com", "User@1234");
    when(userService.login(request)).thenThrow(new UnauthorizedException("Tài khoản không hoạt động"));

    mockMvc.perform(postJson("/api/auth/login",
            "{\"email\":\"inactive@example.com\",\"password\":\"User@1234\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void loginMapsLockedAccountToUnauthorized() throws Exception {
    LoginRequest request = new LoginRequest("locked@example.com", "User@1234");
    when(userService.login(request)).thenThrow(new UnauthorizedException("Tài khoản bị khóa"));

    mockMvc.perform(postJson("/api/auth/login",
            "{\"email\":\"locked@example.com\",\"password\":\"User@1234\"}"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  void loginReturnsBadRequestForMalformedEmail() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"invalid-email\",\"password\":\"Student@123\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void studentRegistrationReturnsCreatedForValidRequest() throws Exception {
    when(userService.addUser(any(UserRegisterRequest.class))).thenReturn(user("STUDENT"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"student01",
                      "password":"Student@123",
                      "fullName":"Student One",
                      "email":"student@example.com",
                      "phoneNumber":"0901234567",
                      "dateOfBirth":"2000-01-01",
                      "gender":"MALE"
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(201))
        .andExpect(jsonPath("$.data.roleCode").value("STUDENT"));
  }

  @Test
  void studentRegistrationReturnsBadRequestForInvalidEmailPasswordAndPhone() throws Exception {
    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"student01",
                      "password":"password",
                      "fullName":"Student One",
                      "email":"student-at-example",
                      "phoneNumber":"123",
                      "dateOfBirth":"2000-01-01"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void studentRegistrationMapsDuplicateEmailToConflict() throws Exception {
    when(userService.addUser(any(UserRegisterRequest.class)))
        .thenThrow(new DuplicateResourceException("Email đã tồn tại"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"student01",
                      "password":"Student@123",
                      "fullName":"Student One",
                      "email":"student@example.com",
                      "phoneNumber":"0901234567"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409));
  }

  @Test
  void teacherRegistrationReturnsCreatedForValidRequest() throws Exception {
    when(userService.registerTeacher(any(TeacherRegisterRequest.class)))
        .thenReturn(user("student@example.com", "TEACHER", AccountStatus.INACTIVE));

    mockMvc
        .perform(
            post("/api/auth/teacher/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"teacher01",
                      "password":"Teacher@123",
                      "fullName":"Teacher One",
                      "email":"student@example.com",
                      "phoneNumber":"0901234567",
                      "specialization":"English",
                      "degree":"Master",
                      "experienceYears":5
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.data.roleCode").value("TEACHER"))
        .andExpect(jsonPath("$.data.status").value("INACTIVE"));
  }

  @Test
  void teacherRegistrationRejectsInvalidPhoneAndNegativeExperience() throws Exception {
    mockMvc.perform(postJson("/api/auth/teacher/register", """
        {
          "username":"teacher01","password":"Teacher@123","fullName":"Teacher One",
          "email":"teacher@example.com","phoneNumber":"123","experienceYears":-1
        }
        """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void teacherRegistrationMapsDuplicateAccountToConflict() throws Exception {
    when(userService.registerTeacher(any(TeacherRegisterRequest.class)))
        .thenThrow(new DuplicateResourceException("Email đã tồn tại"));

    mockMvc.perform(postJson("/api/auth/teacher/register", """
        {
          "username":"teacher01","password":"Teacher@123","fullName":"Teacher One",
          "email":"teacher@example.com","phoneNumber":"0901234567","experienceYears":1
        }
        """))
        .andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void getProfileReturnsAuthenticatedUser() throws Exception {
    when(userService.getCurrentUserProfile(any(Principal.class))).thenReturn(user("STUDENT"));

    mockMvc
        .perform(get("/api/auth/me").principal(() -> "student@example.com"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("student@example.com"));
  }

  @Test
  void getProfileReturnsUnauthorizedWithoutAuthentication() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void changePasswordReturnsSuccessForValidRequest() throws Exception {
    mockMvc
        .perform(
            put("/api/auth/change-password")
                .principal(() -> "student@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "currentPassword": "Old@1234",
                      "newPassword": "New@1234",
                      "confirmPassword": "New@1234"
                    }
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(200))
        .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));

    verify(userService)
        .changePassword(any(Principal.class), eq(new ChangePasswordRequest("Old@1234", "New@1234", "New@1234")));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void changePasswordReturnsBadRequestForWeakPassword() throws Exception {
    mockMvc
        .perform(
            put("/api/auth/change-password")
                .principal(() -> "student@example.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "currentPassword": "Old@1234",
                      "newPassword": "password",
                      "confirmPassword": "password"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void changePasswordReturnsUnauthorizedWithoutAuthentication() throws Exception {
    mockMvc
        .perform(
            put("/api/auth/change-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "currentPassword": "Old@1234",
                      "newPassword": "New@1234",
                      "confirmPassword": "New@1234"
                    }
                    """))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.status").value(401));
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void changePasswordMapsIncorrectCurrentPasswordToBadRequest() throws Exception {
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Mật khẩu hiện tại không chính xác"))
        .when(userService).changePassword(any(Principal.class), any(ChangePasswordRequest.class));

    mockMvc.perform(validChangePassword()).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void changePasswordMapsMismatchedConfirmationToBadRequest() throws Exception {
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Xác nhận mật khẩu mới không khớp"))
        .when(userService).changePassword(any(Principal.class), any(ChangePasswordRequest.class));

    mockMvc.perform(validChangePassword()).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void changePasswordMapsReusedPasswordToBadRequest() throws Exception {
    org.mockito.Mockito.doThrow(new IllegalArgumentException("Mật khẩu mới phải khác mật khẩu hiện tại"))
        .when(userService).changePassword(any(Principal.class), any(ChangePasswordRequest.class));

    mockMvc.perform(validChangePassword()).andExpect(status().isBadRequest());
  }

  @Test
  void malformedJsonReturnsBadRequest() throws Exception {
    mockMvc.perform(postJson("/api/auth/register", "{invalid-json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postJson(
      String path, String json) {
    return post(path).contentType(MediaType.APPLICATION_JSON).content(json);
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validChangePassword() {
    return put("/api/auth/change-password")
        .principal(() -> "student@example.com")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
            {"currentPassword":"Old@1234","newPassword":"New@1234","confirmPassword":"New@1234"}
            """);
  }

  private UserResponse user(String roleCode) {
    return user("student@example.com", roleCode, AccountStatus.ACTIVE);
  }

  private UserResponse user(String email, String roleCode, AccountStatus status) {
    return new UserResponse(
        1,
        "account",
        "Test User",
        email,
        "0901234567",
        null,
        roleCode,
        roleCode,
        status,
        null,
        null);
  }
}
