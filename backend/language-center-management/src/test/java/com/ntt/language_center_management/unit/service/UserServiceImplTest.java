package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.*;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.mapper.UserMapper;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.impl.UserServiceImpl;
import com.ntt.language_center_management.security.CurrentUserResolver;
import java.security.Principal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceImplTest {
  private UserRepository users;
  private StudentRepository students;
  private TeacherRepository teachers;
  private RoleRepository roles;
  private PasswordEncoder encoder;
  private UserServiceImpl service;
  private final Principal principal = () -> "student@example.com";

  @BeforeEach
  void setUp() {
    users = mock(UserRepository.class);
    students = mock(StudentRepository.class);
    teachers = mock(TeacherRepository.class);
    roles = mock(RoleRepository.class);
    encoder = mock(PasswordEncoder.class);
    service = new UserServiceImpl(users, students, teachers, roles, new UserMapper(), encoder,
        new CurrentUserResolver(users, students, teachers));
  }

  @Test
  void shouldReturnMappedUserWhenLoginIsSuccessful() {
    User user = activeUser("STUDENT");
    authenticate("student@example.com", user);
    UserResponse result = service.login(new LoginRequest("student@example.com", "secret"));
    assertThat(result.email()).isEqualTo("student@example.com");
    assertThat(result.roleCode()).isEqualTo("STUDENT");
  }

  @Test
  void shouldReturnUnauthorizedWhenEmailIsMissingOrPasswordIsWrong() {
    when(users.findByEmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
    assertThrows(UnauthorizedException.class,
        () -> service.login(new LoginRequest("missing@example.com", "secret")));
    User user = activeUser("STUDENT");
    when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
    when(encoder.matches("wrong", "hash")).thenReturn(false);
    assertThrows(UnauthorizedException.class,
        () -> service.login(new LoginRequest(user.getEmail(), "wrong")));
  }

  @Test
  void shouldReturnUnauthorizedWhenAccountIsInactiveOrLocked() {
    for (AccountStatus status : List.of(AccountStatus.INACTIVE, AccountStatus.LOCKED)) {
      User user = activeUser("STUDENT");
      user.setStatus(status);
      authenticate(status.name(), user);
      assertThrows(UnauthorizedException.class,
          () -> service.login(new LoginRequest(status.name(), "secret")));
    }
  }

  @Test
  void shouldAcceptOnlyExpectedRoleWhenUsingAdminOrStaffLogin() {
    User admin = activeUser("ADMIN");
    User consultant = activeUser("CONSULTANT");
    authenticate("admin@example.com", admin);
    authenticate("staff@example.com", consultant);
    assertThat(service.loginAdmin(new LoginRequest("admin@example.com", "secret")).roleCode())
        .isEqualTo("ADMIN");
    assertThat(service.loginStaff(new LoginRequest("staff@example.com", "secret")).roleCode())
        .isEqualTo("CONSULTANT");
    assertThrows(UnauthorizedException.class,
        () -> service.loginAdmin(new LoginRequest("staff@example.com", "secret")));
    assertThrows(UnauthorizedException.class,
        () -> service.loginStaff(new LoginRequest("admin@example.com", "secret")));
  }

  @Test
  void shouldNormalizeAndCreateBothUserAndStudentWhenRegistrationIsValid() {
    when(roles.findByRoleCodeIgnoreCase("STUDENT")).thenReturn(Optional.of(role("STUDENT")));
    when(encoder.encode("Secret@1")).thenReturn("encoded");
    when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    when(students.save(any())).thenAnswer(i -> i.getArgument(0));
    service.addUser(studentRequest("  NewStudent  ", "  STUDENT@Example.COM  "));

    ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
    ArgumentCaptor<Student> student = ArgumentCaptor.forClass(Student.class);
    verify(users).existsByEmailIgnoreCase("student@example.com");
    verify(users).existsByUsernameIgnoreCase("NewStudent");
    verify(users).save(user.capture());
    verify(students).save(student.capture());
    assertThat(user.getValue().getEmail()).isEqualTo("student@example.com");
    assertThat(user.getValue().getUsername()).isEqualTo("NewStudent");
    assertThat(user.getValue().getPasswordHash()).isEqualTo("encoded");
    assertThat(student.getValue().getUserId()).isSameAs(user.getValue());
    assertThat(student.getValue().getStudentCode()).matches("HV[A-F0-9]{10}");
  }

  @Test
  void shouldRejectRegistrationBeforeSavingWhenEmailOrUsernameExists() {
    UserRegisterRequest request = studentRequest("student", "student@example.com");
    when(users.existsByEmailIgnoreCase("student@example.com")).thenReturn(true);
    assertThrows(DuplicateResourceException.class, () -> service.addUser(request));
    reset(users);
    when(users.existsByUsernameIgnoreCase("student")).thenReturn(true);
    assertThrows(DuplicateResourceException.class, () -> service.addUser(request));
    verify(users, never()).save(any());
    verify(students, never()).save(any());
  }

  @Test
  void shouldRejectRegistrationBeforeSavingWhenStudentRoleIsMissing() {
    when(roles.findByRoleCodeIgnoreCase("STUDENT")).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class,
        () -> service.addUser(studentRequest("student", "student@example.com")));
    verify(users, never()).save(any());
  }

  @Test
  void shouldGenerateUniqueStudentCodesWhenRegisteringStudents() {
    stubStudentRegistration();
    service.addUser(studentRequest("one", "one@example.com"));
    service.addUser(studentRequest("two", "two@example.com"));
    ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
    verify(students, times(2)).save(captor.capture());
    assertThat(captor.getAllValues()).extracting(Student::getStudentCode).doesNotHaveDuplicates();
  }

  @Test
  void shouldCreateInactiveTeacherWithUniqueCodeWhenRegistrationIsValid() {
    when(roles.findByRoleCodeIgnoreCase("TEACHER")).thenReturn(Optional.of(role("TEACHER")));
    when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    when(teachers.save(any())).thenAnswer(i -> i.getArgument(0));
    service.registerTeacher(teacherRequest(" teacher1 ", " TEACHER1@Example.COM "));
    service.registerTeacher(teacherRequest("teacher2", "teacher2@example.com"));

    ArgumentCaptor<User> user = ArgumentCaptor.forClass(User.class);
    ArgumentCaptor<Teacher> teacher = ArgumentCaptor.forClass(Teacher.class);
    verify(users, times(2)).save(user.capture());
    verify(teachers, times(2)).save(teacher.capture());
    assertThat(user.getAllValues().get(0).getStatus()).isEqualTo(AccountStatus.INACTIVE);
    assertThat(user.getAllValues().get(0).getEmail()).isEqualTo("teacher1@example.com");
    assertThat(teacher.getAllValues()).extracting(Teacher::getTeacherCode).doesNotHaveDuplicates();
    assertThat(teacher.getAllValues().get(0).getUserId()).isSameAs(user.getAllValues().get(0));
  }

  @Test
  void shouldRejectProfileOperationsWhenPrincipalIsNullOrBlank() {
    Principal blank = () -> "  ";
    assertThrows(UnauthorizedException.class, () -> service.getCurrentUserProfile(null));
    assertThrows(UnauthorizedException.class, () -> service.getCurrentUserProfile(blank));
    assertThrows(UnauthorizedException.class, () -> service.getStudentProfile(null));
    assertThrows(UnauthorizedException.class, () -> service.getStudentProfile(blank));
  }

  @Test
  void shouldGetAndUpdateStudentProfileWhenPrincipalIsValid() {
    Student student = studentProfile();
    when(students.findByUserId_EmailIgnoreCase(principal.getName())).thenReturn(Optional.of(student));
    when(students.save(student)).thenReturn(student);
    assertThat(service.getStudentProfile(principal).studentCode()).isEqualTo("HV001");

    StudentProfileResponse result = service.updateStudentProfile(principal,
        new StudentProfileUpdateRequest("  New Name  ", " 0901234567 ", "   ", new Date(0),
            Gender.MALE, "  avatar.png  "));
    assertThat(result.fullName()).isEqualTo("New Name");
    assertThat(result.phoneNumber()).isEqualTo("0901234567");
    assertThat(result.address()).isNull();
    assertThat(result.avatar()).isEqualTo("avatar.png");
    verify(users).save(student.getUserId());
    verify(students).save(student);
  }

  @Test
  void shouldEncodeAndSaveNewPasswordWhenChangeIsValid() {
    User user = activeUser("STUDENT");
    when(users.findByEmailIgnoreCase(principal.getName())).thenReturn(Optional.of(user));
    when(encoder.matches("Old@1234", "hash")).thenReturn(true);
    when(encoder.matches("New@1234", "hash")).thenReturn(false);
    when(encoder.encode("New@1234")).thenReturn("new-hash");
    service.changePassword(principal,
        new ChangePasswordRequest("Old@1234", "New@1234", "New@1234"));
    assertThat(user.getPasswordHash()).isEqualTo("new-hash");
    verify(users).save(user);
  }

  @Test
  void shouldRejectPasswordChangeWhenCredentialsAreInvalid() {
    User user = activeUser("STUDENT");
    when(users.findByEmailIgnoreCase(principal.getName())).thenReturn(Optional.of(user));
    when(encoder.matches("wrong", "hash")).thenReturn(false);
    assertThrows(IllegalArgumentException.class, () -> service.changePassword(principal,
        new ChangePasswordRequest("wrong", "New@1234", "New@1234")));
    when(encoder.matches("old", "hash")).thenReturn(true);
    assertThrows(IllegalArgumentException.class, () -> service.changePassword(principal,
        new ChangePasswordRequest("old", "New@1234", "Other@123")));
    assertThrows(IllegalArgumentException.class, () -> service.changePassword(principal,
        new ChangePasswordRequest("old", "old", "old")));
    verify(users, never()).save(any());
  }

  @Test
  void shouldNormalizeFiltersAndPageRequestWhenSearchingUsers() {
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    when(users.searchAdminUsers(eq("student"), eq("CONSULTANT"), eq(AccountStatus.ACTIVE), any()))
        .thenReturn(new PageImpl<>(List.of(activeUser("STUDENT"))));
    PageResponse<UserResponse> result = service.searchUsers(" student ", " consultant ",
        " active ", 0, 20, "invalid", "asc");
    verify(users).searchAdminUsers(eq("student"), eq("CONSULTANT"), eq(AccountStatus.ACTIVE),
        pageable.capture());
    assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
    assertThat(pageable.getValue().getSort().getOrderFor("createdAt").isAscending()).isTrue();
    assertThat(result.content()).hasSize(1);
  }

  @Test
  void shouldRejectSearchWhenStatusIsInvalid() {
    assertThrows(IllegalArgumentException.class,
        () -> service.searchUsers(null, null, "deleted", 0, 20, "id", "desc"));
    verify(users, never()).searchAdminUsers(any(), any(), any(), any());
  }

  @Test
  void shouldReturnResourceNotFoundWhenChangingStatusOfMissingUser() {
    when(users.findById(999)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class,
        () -> service.changeStatus(999, AccountStatus.LOCKED));
    verify(users, never()).save(any());
  }

  private void authenticate(String email, User user) {
    when(users.findByEmailIgnoreCase(email)).thenReturn(Optional.of(user));
    when(encoder.matches("secret", "hash")).thenReturn(true);
  }

  private User activeUser(String roleCode) {
    User user = new User();
    user.setId(1); user.setUsername("student"); user.setFullName("Student Name");
    user.setEmail("student@example.com"); user.setPasswordHash("hash");
    user.setStatus(AccountStatus.ACTIVE); user.setRoleId(role(roleCode));
    user.setCreatedAt(new Date()); user.setUpdatedAt(new Date());
    return user;
  }

  private Role role(String code) { return new Role(1, code, code); }

  private UserRegisterRequest studentRequest(String username, String email) {
    return new UserRegisterRequest(username, "Secret@1", "Student Name", email,
        "0901234567", "Address", new Date(0), Gender.FEMALE, null);
  }

  private TeacherRegisterRequest teacherRequest(String username, String email) {
    return new TeacherRegisterRequest(username, "Secret@1", "Teacher Name", email,
        "0901234567", "Address", "English", "Master", 5);
  }

  private void stubStudentRegistration() {
    when(roles.findByRoleCodeIgnoreCase("STUDENT")).thenReturn(Optional.of(role("STUDENT")));
    when(users.save(any())).thenAnswer(i -> i.getArgument(0));
    when(students.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  private Student studentProfile() {
    Student student = new Student(1, "HV001");
    student.setUserId(activeUser("STUDENT"));
    student.setDateOfBirth(new Date(0)); student.setGender(Gender.FEMALE);
    student.setAvatar("old.png");
    return student;
  }
}
