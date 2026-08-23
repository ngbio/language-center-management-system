package com.ntt.language_center_management.service.impl;

import java.security.Principal;
import java.util.Date;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.request.TeacherRegisterRequest;
import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.mapper.UserMapper;
import com.ntt.language_center_management.repository.RoleRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.UserService;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_ROLE_CODE = "STUDENT";
    private static final String TEACHER_ROLE_CODE = "TEACHER";

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
            UserRepository userRepository,
            StudentRepository studentRepository,
            TeacherRepository teacherRepository,
            RoleRepository roleRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
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
        User user = userRepository.findByEmailIgnoreCase(request.email()).orElseThrow(() -> new UnauthorizedException(
                        "Email hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Email hoặc mật khẩu không chính xác");
        }

        validateUserCanLogin(user);
        return userMapper.toResponse(user);
    }

    private void validateUserCanLogin(User user) {
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new UnauthorizedException("Tài khoản không ở trạng thái hoạt động");
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

        User savedUser = userRepository.save(user);

        Student student = new Student();
        student.setStudentCode(generateStudentCode());
        student.setDateOfBirth(request.dateOfBirth());
        student.setGender(request.gender());
        student.setAvatar(request.avatar());
        student.setUserId(savedUser);
        studentRepository.save(student);

        // TODO: Gửi email xác nhận sau khi transaction commit khi đã cấu hình email service.
        return userMapper.toResponse(savedUser);
    }

    @Override
    public UserResponse addTeacher(TeacherRegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new DuplicateResourceException("Email này đã có người đăng ký!");
        }
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new DuplicateResourceException("Tên đăng nhập này đã tồn tại!");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        user.setPhoneNumber(request.phoneNumber());
        user.setAddress(request.address());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        Date now = new Date();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setStatus(AccountStatus.ACTIVE);
        user.setRoleId(roleRepository.findByRoleCodeIgnoreCase(TEACHER_ROLE_CODE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy role mặc định: " + TEACHER_ROLE_CODE)));

        User savedUser = userRepository.save(user);

        Teacher teacher = new Teacher();
        teacher.setTeacherCode(generateTeacherCode());
        teacher.setSpecialization(request.specialization());
        teacher.setDegree(request.degree());
        teacher.setExperienceYears(request.experienceYears());
        teacher.setUserId(savedUser);
        teacherRepository.save(teacher);

        return userMapper.toResponse(savedUser);
    }

    private String generateStudentCode() {
        return "HV" + java.util.UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }

    private String generateTeacherCode() {
        return "GV" + java.util.UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 10)
                .toUpperCase();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUserProfile(Principal principal) {
        User user = validateAndGetCurrentUser(principal);
        return userMapper.toResponse(user);
    }

    private User validateAndGetCurrentUser(Principal principal) {
        if (principal == null || !StringUtils.hasText(principal.getName())) {
            throw new UnauthorizedException("Không thể xác định người dùng hiện tại");
        }

        return getUserEntityByEmail(principal.getName());
    }
}
