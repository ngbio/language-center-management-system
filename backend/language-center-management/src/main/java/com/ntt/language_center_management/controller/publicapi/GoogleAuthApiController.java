package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.request.GoogleLoginRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.LoginResponse;
import com.ntt.language_center_management.service.GoogleAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/google")
public class GoogleAuthApiController {
  private final GoogleAuthService googleAuth;

  public GoogleAuthApiController(GoogleAuthService googleAuth) {
    this.googleAuth = googleAuth;
  }

  @PostMapping
  public ApiResponse<LoginResponse> login(@Valid @RequestBody GoogleLoginRequest request) {
    return new ApiResponse<>(200, "Đăng nhập Google thành công", googleAuth.login(request));
  }
}
