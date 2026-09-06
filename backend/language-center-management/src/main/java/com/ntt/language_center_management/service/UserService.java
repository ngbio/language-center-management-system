package com.ntt.language_center_management.service;

import java.security.Principal;
import java.util.List;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.request.TeacherRegisterRequest;
import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.dto.response.StudentProfileResponse;
import com.ntt.language_center_management.dto.request.StudentProfileUpdateRequest;
import com.ntt.language_center_management.entity.User;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Integer id);

    User getUserEntityByEmail(String email);

    Long countUsers();

    UserResponse login(LoginRequest request);

    UserResponse loginAdmin(LoginRequest request);

    UserResponse loginStaff(LoginRequest request);

    UserResponse addUser(UserRegisterRequest request);

    UserResponse addTeacher(TeacherRegisterRequest request);

    UserResponse registerTeacher(TeacherRegisterRequest request);

    UserResponse getCurrentUserProfile(Principal principal);

    StudentProfileResponse getStudentProfile(Principal principal);

    StudentProfileResponse updateStudentProfile(Principal principal, StudentProfileUpdateRequest request);

    PageResponse<UserResponse> searchUsers(
        String keyword,
        String roleCode,
        String status,
        int page,
        int size,
        String sort,
        String direction);

    UserResponse changeStatus(Integer id, String status);
}
