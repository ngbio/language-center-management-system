package com.ntt.language_center_management.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ntt.language_center_management.dto.request.TeacherRegisterRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class ApiAdminUserController {

    private final UserService userService;

    public ApiAdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/teachers")
    public ResponseEntity<ApiResponse<UserResponse>> registerTeacher(
            @Valid @RequestBody TeacherRegisterRequest request) {
        UserResponse user = userService.addTeacher(request);
        return new ResponseEntity<>(
                new ApiResponse<>(
                        HttpStatus.CREATED.value(),
                        "Tạo tài khoản giáo viên thành công",
                        user),
                HttpStatus.CREATED);
    }
}
