package com.ntt.language_center_management.controller.publicapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.dto.request.TeacherRegisterRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.LoginResponse;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import com.ntt.language_center_management.exception.UnauthorizedException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class ApiUserController {

    private final UserService userService;
    private final JwtUtils jwtUtils;

    public ApiUserController(UserService userService, JwtUtils jwtUtils) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        UserResponse user = userService.login(request);

        String token = jwtUtils.generateToken(user.email());
        LoginResponse loginResponse = new LoginResponse(
                token,
                user.id(),
                user.email(),
                user.roleName(),
                user.roleCode());
        ApiResponse<LoginResponse> response = new ApiResponse<>(
                HttpStatus.OK.value(),
                "Đăng nhập học viên hoặc giáo viên thành công",
                loginResponse);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserRegisterRequest request) {
        UserResponse user = userService.addUser(request);
        return new ResponseEntity<>(
            new ApiResponse<>(HttpStatus.CREATED.value(), "Đăng ký thành công", user),
            HttpStatus.CREATED);
    }

    @PostMapping("/teacher/register")
    public ResponseEntity<ApiResponse<UserResponse>> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request) {
        UserResponse user = userService.registerTeacher(request);
        return new ResponseEntity<>(
                new ApiResponse<>(
                        HttpStatus.CREATED.value(),
                        "Đăng ký giáo viên thành công. Vui lòng chờ quản trị viên kích hoạt tài khoản.",
                        user),
                HttpStatus.CREATED);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Chưa xác thực người dùng");
        }
        UserResponse user = this.userService.getCurrentUserProfile(principal);
        return ResponseEntity.ok(new ApiResponse<>(
                HttpStatus.OK.value(),
                "Lấy thông tin người dùng thành công",
                user));
    }

}
