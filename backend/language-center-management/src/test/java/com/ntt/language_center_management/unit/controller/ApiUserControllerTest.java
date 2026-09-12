package com.ntt.language_center_management.unit.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.controller.publicapi.ApiUserController;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.service.PasswordResetService;
import com.ntt.language_center_management.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ApiUserControllerTest {

  @Test
  void shouldReturnTokenAndConsistentResponseWhenLoginSucceeds() {
    UserService userService = mock(UserService.class);
    JwtUtils jwtUtils = mock(JwtUtils.class);
    LoginRequest request = new LoginRequest("admin@example.com", "secret");
    UserResponse user =
        new UserResponse(
            1,
            "admin",
            "Admin",
            "admin@example.com",
            null,
            null,
            "Admin",
            "ADMIN",
            AccountStatus.ACTIVE,
            null,
            null);
    when(userService.login(request)).thenReturn(user);
    when(jwtUtils.generateToken(user.email())).thenReturn("jwt-token");

    var response = new ApiUserController(userService, jwtUtils, mock(PasswordResetService.class))
        .login(request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(200, response.getBody().status());
    assertEquals("jwt-token", response.getBody().data().token());
  }
}
