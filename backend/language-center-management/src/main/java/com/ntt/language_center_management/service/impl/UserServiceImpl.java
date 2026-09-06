package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.LoginRequest;
import com.ntt.language_center_management.dto.request.TeacherRegisterRequest;
import com.ntt.language_center_management.dto.request.UserRegisterRequest;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.dto.response.StudentProfileResponse;
import com.ntt.language_center_management.dto.request.StudentProfileUpdateRequest;
import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.mapper.UserMapper;
import com.ntt.language_center_management.repository.RoleRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.UserService;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class UserServiceImpl implements UserService {

  private static final String DEFAULT_ROLE_CODE = "STUDENT";
  private static final String TEACHER_ROLE_CODE = "TEACHER";
  private static final Set<String> USER_STATUSES = Set.of("ACTIVE", "INACTIVE", "LOCKED");
  private static final Set<String> USER_SORT_FIELDS =
      Set.of("id", "username", "fullName", "email", "status", "createdAt");

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
    return userRepository.findAll().stream().map(userMapper::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getUserById(Integer id) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy người dùng có ID: " + id));

    return userMapper.toResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public User getUserEntityByEmail(String email) {
    return userRepository
        .findByEmailIgnoreCase(email)
        .orElseThrow(
            () -> new ResourceNotFoundException("Không tìm thấy người dùng có email: " + email));
  }

  @Override
  @Transactional(readOnly = true)
  public Long countUsers() {
    return userRepository.count();
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse login(LoginRequest request) {
    User user = authenticate(request);
    if (user.getRoleId() == null
        || !Set.of(DEFAULT_ROLE_CODE, TEACHER_ROLE_CODE)
            .contains(user.getRoleId().getRoleCode())) {
      throw new UnauthorizedException("Cổng đăng nhập này chỉ dành cho học viên và giáo viên");
    }
    return userMapper.toResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse loginAdmin(LoginRequest request) {
    User user = authenticate(request);
    if (user.getRoleId() == null || !"ADMIN".equals(user.getRoleId().getRoleCode())) {
      throw new UnauthorizedException("Tài khoản không có quyền quản trị");
    }
    return userMapper.toResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse loginStaff(LoginRequest request) {
    User user = authenticate(request);
    if (user.getRoleId() == null || !"CONSULTANT".equals(user.getRoleId().getRoleCode())) {
      throw new UnauthorizedException("Tài khoản không có quyền nhân viên tư vấn");
    }
    return userMapper.toResponse(user);
  }

  private User authenticate(LoginRequest request) {
    User user =
        userRepository
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new UnauthorizedException("Email hoặc mật khẩu không chính xác"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Email hoặc mật khẩu không chính xác");
    }

    validateUserCanLogin(user);
    return user;
  }

  private void validateUserCanLogin(User user) {
    if (!"ACTIVE".equals(user.getStatus())) {
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
    user.setStatus("ACTIVE");

    Role studentRole =
        roleRepository
            .findByRoleCodeIgnoreCase(DEFAULT_ROLE_CODE)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
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
    return createTeacher(request, "ACTIVE");
  }

  @Override
  public UserResponse registerTeacher(TeacherRegisterRequest request) {
    return createTeacher(request, "INACTIVE");
  }

  private UserResponse createTeacher(TeacherRegisterRequest request, String initialStatus) {
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
    user.setStatus(initialStatus);
    user.setRoleId(
        roleRepository
            .findByRoleCodeIgnoreCase(TEACHER_ROLE_CODE)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
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
    return "HV"
        + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
  }

  private String generateTeacherCode() {
    return "GV"
        + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponse getCurrentUserProfile(Principal principal) {
    User user = validateAndGetCurrentUser(principal);
    return userMapper.toResponse(user);
  }

  @Override
  @Transactional(readOnly = true)
  public StudentProfileResponse getStudentProfile(Principal principal) {
    return toStudentProfile(findCurrentStudent(principal));
  }

  @Override
  public StudentProfileResponse updateStudentProfile(
      Principal principal, StudentProfileUpdateRequest request) {
    Student student = findCurrentStudent(principal);
    User user = student.getUserId();
    user.setFullName(request.fullName().trim());
    user.setPhoneNumber(trimToNull(request.phoneNumber()));
    user.setAddress(trimToNull(request.address()));
    user.setUpdatedAt(new Date());
    student.setDateOfBirth(request.dateOfBirth());
    student.setGender(trimToNull(request.gender()));
    student.setAvatar(trimToNull(request.avatar()));
    userRepository.save(user);
    return toStudentProfile(studentRepository.save(student));
  }

  private Student findCurrentStudent(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new UnauthorizedException("Không thể xác định học viên hiện tại");
    }
    return studentRepository.findByUserId_EmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ học viên"));
  }

  private StudentProfileResponse toStudentProfile(Student student) {
    User user = student.getUserId();
    return new StudentProfileResponse(
        student.getId(), student.getStudentCode(), user.getId(), user.getUsername(),
        user.getFullName(), user.getEmail(), user.getPhoneNumber(), user.getAddress(),
        student.getDateOfBirth(), student.getGender(), student.getAvatar(), user.getStatus(),
        user.getCreatedAt(), user.getUpdatedAt());
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }

  private User validateAndGetCurrentUser(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new UnauthorizedException("Không thể xác định người dùng hiện tại");
    }

    return getUserEntityByEmail(principal.getName());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<UserResponse> searchUsers(
      String keyword,
      String roleCode,
      String status,
      int page,
      int size,
      String sort,
      String direction) {
    if (page < 0) {
      throw new IllegalArgumentException("Số trang không được nhỏ hơn 0");
    }
    if (size < 1 || size > 100) {
      throw new IllegalArgumentException("Kích thước trang phải từ 1 đến 100");
    }

    String sortField = USER_SORT_FIELDS.contains(sort) ? sort : "createdAt";
    Sort.Direction sortDirection =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;

    Page<UserResponse> result =
        userRepository
            .searchAdminUsers(
                normalizeFilter(keyword, false),
                normalizeFilter(roleCode, true),
                normalizeStatusFilter(status),
                PageRequest.of(page, size, Sort.by(sortDirection, sortField)))
            .map(userMapper::toResponse);

    return PageResponse.from(result);
  }

  @Override
  public UserResponse changeStatus(Integer id, String status) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException(
                    "Không tìm thấy người dùng có ID: " + id));

    if (status == null || status.isBlank()) {
      throw new IllegalArgumentException("Trạng thái không được để trống");
    }
    if (!USER_STATUSES.contains(status)) {
      throw new IllegalArgumentException(
          "Trạng thái phải là ACTIVE, INACTIVE hoặc LOCKED");
    }

    user.setStatus(status);
    user.setUpdatedAt(new Date());
    return userMapper.toResponse(userRepository.save(user));
  }

  private String normalizeFilter(String value, boolean uppercase) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    String normalized = value.trim();
    return uppercase ? normalized.toUpperCase(Locale.ROOT) : normalized;
  }

  private String normalizeStatusFilter(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }

    String normalized = status.trim().toUpperCase(Locale.ROOT);
    if (!USER_STATUSES.contains(normalized)) {
      throw new IllegalArgumentException(
          "Trạng thái phải là ACTIVE, INACTIVE hoặc LOCKED");
    }
    return normalized;
  }
}
