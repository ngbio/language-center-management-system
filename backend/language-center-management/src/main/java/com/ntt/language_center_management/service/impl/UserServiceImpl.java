package com.ntt.language_center_management.service.impl;

import java.util.Date;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.UserMapper;
import com.ntt.language_center_management.repository.RoleRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.UserService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE_CODE = "STUDENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng có ID: " + id));

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy người dùng có email: " + email));
    }

    @Override
    @Transactional(readOnly = true)
    public Long countUsers() {
        return userRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow(() -> new IllegalArgumentException(
                        "Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác");
        }

        validateUserCanLogin(user);
        return userMapper.toResponse(user);
    }

    private void validateUserCanLogin(User user) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Tài khoản không ở trạng thái hoạt động");
        }
    }

    @Override
    public UserResponse addUser(UserRegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email này đã có người đăng ký!");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Tên đăng nhập này đã tồn tại!");
        }

        User user = userMapper.toEntity(request);
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        Date now = new Date();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setStatus(AccountStatus.ACTIVE);

        Role studentRole = roleRepository.findByRoleCodeIgnoreCase(DEFAULT_ROLE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy role mặc định: " + DEFAULT_ROLE_CODE));
        user.setRoleId(studentRole);

        // TODO: Tạo Student và sinh studentCode khi đã thống nhất quy tắc mã học viên.
        // TODO: Upload avatar vào Student.avatar khi đã cấu hình dịch vụ lưu trữ.

        User savedUser = userRepository.save(user);

        // TODO: Gửi email xác nhận sau khi transaction commit khi đã cấu hình email service.
        return userMapper.toResponse(savedUser);
    }
}
