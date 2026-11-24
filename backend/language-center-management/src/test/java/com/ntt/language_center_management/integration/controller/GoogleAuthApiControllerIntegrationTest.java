package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.publicapi.GoogleAuthApiController;
import com.ntt.language_center_management.dto.response.LoginResponse;
import com.ntt.language_center_management.exception.GoogleAccountLinkRequiredException;
import com.ntt.language_center_management.exception.GoogleSignInUnavailableException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.service.GoogleAuthService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GoogleAuthApiController.class)
@Import(SecurityConfig.class)
class GoogleAuthApiControllerIntegrationTest {
  @Autowired MockMvc mvc;
  @MockitoBean GoogleAuthService googleAuth;
  @MockitoBean UserService users;
  @MockitoBean JwtUtils jwtUtils;

  @Test
  void publicEndpointReturnsExistingJwtResponseContract() throws Exception {
    when(googleAuth.login(any())).thenReturn(new LoginResponse("app-jwt", 7,
        "student@example.com", "Học viên", "STUDENT"));
    mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
            .content("{\"idToken\":\"firebase-id-token\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.token").value("app-jwt"))
        .andExpect(jsonPath("$.data.roleCode").value("STUDENT"));
  }

  @Test
  void rejectsMissingTokenBeforeCallingService() throws Exception {
    mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(googleAuth);
  }

  @Test
  void existingEmailRequiresExplicitLinkConfirmation() throws Exception {
    when(googleAuth.login(any())).thenThrow(new GoogleAccountLinkRequiredException());
    mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
            .content("{\"idToken\":\"firebase-id-token\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.data.requiresPassword").value(true))
        .andExpect(jsonPath("$.data.token").doesNotExist());
  }

  @Test
  void invalidIdentityDoesNotIssueJwt() throws Exception {
    when(googleAuth.login(any())).thenThrow(new UnauthorizedException("Invalid identity"));
    mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
            .content("{\"idToken\":\"bad-token\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void disabledFirebaseReturns503() throws Exception {
    when(googleAuth.login(any())).thenThrow(new GoogleSignInUnavailableException());
    mvc.perform(post("/api/auth/google").contentType(MediaType.APPLICATION_JSON)
            .content("{\"idToken\":\"firebase-id-token\"}"))
        .andExpect(status().isServiceUnavailable());
  }
}
