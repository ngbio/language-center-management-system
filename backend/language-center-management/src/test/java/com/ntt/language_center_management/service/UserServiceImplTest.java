package com.ntt.language_center_management.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.mapper.UserMapper;
import com.ntt.language_center_management.repository.RoleRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.impl.UserServiceImpl;

import static org.mockito.Mockito.mock;

class UserServiceImplTest {

    private UserRepository userRepository;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userService = new UserServiceImpl(
                userRepository,
                mock(StudentRepository.class),
                mock(TeacherRepository.class),
                mock(RoleRepository.class),
                mock(UserMapper.class),
                mock(PasswordEncoder.class));
    }

    @Test
    void loginReturnsUnauthorizedWhenEmailDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("missing@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedException.class,
                () -> userService.login(new LoginRequest("missing@example.com", "secret")));
    }
}
