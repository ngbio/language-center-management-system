package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.TeacherProfileUpdateRequest;
import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import com.ntt.language_center_management.dto.response.TeacherProfileResponse;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.mapper.TeacherMapper;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.service.impl.TeacherServiceImpl;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TeacherServiceImplTest {

  @Mock private TeacherRepository teacherRepository;
  @Mock private TeacherMapper teacherMapper;

  private TeacherServiceImpl teacherService;

  @BeforeEach
  void setUp() {
    teacherService = new TeacherServiceImpl(teacherRepository, teacherMapper);
  }

  @Test
  void shouldMapRepositoryResultsWhenGettingActiveTeachers() {
    Teacher teacher = teacher();
    TeacherOptionResponse option =
        new TeacherOptionResponse(3, "GV000003", "Nguyen Van A", "English", "Master");
    when(teacherRepository.findByUserStatus(AccountStatus.ACTIVE)).thenReturn(List.of(teacher));
    when(teacherMapper.toOptionResponse(teacher)).thenReturn(option);

    assertThat(teacherService.getActiveTeachers()).containsExactly(option);
    verify(teacherRepository).findByUserStatus(AccountStatus.ACTIVE);
  }

  @Test
  void shouldReturnCurrentTeacherProfileWhenPrincipalIsValid() {
    Teacher teacher = teacher();
    when(teacherRepository.findByUserId_EmailIgnoreCase("teacher@example.com"))
        .thenReturn(Optional.of(teacher));

    TeacherProfileResponse response =
        teacherService.getProfile(() -> "teacher@example.com");

    assertThat(response.teacherId()).isEqualTo(3);
    assertThat(response.email()).isEqualTo("teacher@example.com");
    assertThat(response.status()).isEqualTo("ACTIVE");
  }

  @Test
  void shouldRejectProfileRequestWhenPrincipalIsMissing() {
    assertThatThrownBy(() -> teacherService.getProfile(null))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void shouldRejectProfileRequestWhenPrincipalNameIsBlank() {
    Principal principal = () -> "   ";

    assertThatThrownBy(() -> teacherService.getProfile(principal))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void shouldReportMissingTeacherWhenProfileDoesNotExist() {
    when(teacherRepository.findByUserId_EmailIgnoreCase("missing@example.com"))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> teacherService.getProfile(() -> "missing@example.com"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldTrimValuesAndConvertBlanksWhenUpdatingProfile() {
    Teacher teacher = teacher();
    when(teacherRepository.findByUserId_EmailIgnoreCase("teacher@example.com"))
        .thenReturn(Optional.of(teacher));
    when(teacherRepository.save(teacher)).thenReturn(teacher);
    TeacherProfileUpdateRequest request =
        new TeacherProfileUpdateRequest(
            "  Tran Thi B  ", " 0901234567 ", "   ", " Japanese ", "   ", null);

    TeacherProfileResponse response =
        teacherService.updateProfile(() -> "teacher@example.com", request);

    assertThat(teacher.getUserId().getFullName()).isEqualTo("Tran Thi B");
    assertThat(teacher.getUserId().getPhoneNumber()).isEqualTo("0901234567");
    assertThat(teacher.getUserId().getAddress()).isNull();
    assertThat(teacher.getSpecialization()).isEqualTo("Japanese");
    assertThat(teacher.getDegree()).isNull();
    assertThat(teacher.getExperienceYears()).isZero();
    assertThat(teacher.getUserId().getUpdatedAt()).isNotNull();
    assertThat(response.fullName()).isEqualTo("Tran Thi B");
    verify(teacherRepository).save(teacher);
  }

  private Teacher teacher() {
    User user =
        new User(
            7,
            "teacher",
            "hash",
            "Nguyen Van A",
            "teacher@example.com",
            AccountStatus.ACTIVE,
            new Date());
    Teacher teacher = new Teacher(3, "GV000003", 5);
    teacher.setUserId(user);
    teacher.setSpecialization("English");
    teacher.setDegree("Master");
    return teacher;
  }
}
