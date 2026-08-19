package com.ntt.language_center_management.mapper;

import org.springframework.stereotype.Component;

import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.entity.User;

@Component
public class UserMapper {

    public User toEntity(UserRegisterRequest request) {
        User user = new User();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setAddress(request.address());
        return user;
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
