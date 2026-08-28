package com.ntt.language_center_management.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ntt.language_center_management.dto.request.TeacherRegisterRequest;
import com.ntt.language_center_management.dto.request.ChangeUserStatusRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
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

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "Lấy danh sách người dùng thành công",
                userService.searchUsers(keyword, roleCode, status, page, size, sort, direction));
    }

    @GetMapping("/users/{id}")
    public ApiResponse<UserResponse> getUser(@PathVariable Integer id) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "Lấy thông tin người dùng thành công",
                userService.getUserById(id));
    }

    @PatchMapping("/users/{id}/status")
    public ApiResponse<UserResponse> changeUserStatus(
            @PathVariable Integer id,
            @Valid @RequestBody ChangeUserStatusRequest request) {
        return new ApiResponse<>(
                HttpStatus.OK.value(),
                "Cập nhật trạng thái người dùng thành công",
                userService.changeStatus(id, request.status()));
    }
}
