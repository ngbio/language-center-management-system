package com.ntt.language_center_management.controller.admin;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.LoginResponse;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthApiController {

  private final UserService userService;
  private final JwtUtils jwtUtils;

  public AdminAuthApiController(UserService userService, JwtUtils jwtUtils) {
    this.userService = userService;
    this.jwtUtils = jwtUtils;
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginResponse>> login(
      @Valid @RequestBody LoginRequest request) {
    UserResponse user = userService.loginAdmin(request);
    LoginResponse loginResponse =
        new LoginResponse(
            jwtUtils.generateToken(user.email()),
            user.id(),
            user.email(),
            user.roleName(),
            user.roleCode());

    return ResponseEntity.ok(
        new ApiResponse<>(HttpStatus.OK.value(), "Đăng nhập quản trị thành công", loginResponse));
  }
}
