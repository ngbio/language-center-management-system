package com.ntt.language_center_management.service;

import java.util.List;

import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.entity.User;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Integer id);

    User getUserEntityByEmail(String email);

    Long countUsers();

    UserResponse addUser(UserRegisterRequest request);
}
