package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.AccountStatus;

import com.ntt.language_center_management.dto.request.TeacherProfileUpdateRequest;
import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import com.ntt.language_center_management.dto.response.TeacherProfileResponse;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.mapper.TeacherMapper;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.service.TeacherService;
import com.ntt.language_center_management.security.CurrentUserResolver;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.ntt.language_center_management.util.TextUtils.trimToNull;

@Service
public class TeacherServiceImpl implements TeacherService {
  private final TeacherRepository teacherRepository;
  private final TeacherMapper teacherMapper;
  private final CurrentUserResolver currentUserResolver;

  public TeacherServiceImpl(
      TeacherRepository teacherRepository,
      TeacherMapper teacherMapper,
      CurrentUserResolver currentUserResolver) {
    this.teacherRepository = teacherRepository;
    this.teacherMapper = teacherMapper;
    this.currentUserResolver = currentUserResolver;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TeacherOptionResponse> getActiveTeachers() {
    return teacherRepository.findByUserStatus(AccountStatus.ACTIVE).stream()
        .map(teacherMapper::toOptionResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public TeacherProfileResponse getProfile(Principal principal) {
    return toProfile(findCurrentTeacher(principal));
  }

  @Override
  @Transactional
  public TeacherProfileResponse updateProfile(
      Principal principal, TeacherProfileUpdateRequest request) {
    Teacher teacher = findCurrentTeacher(principal);
    User user = teacher.getUserId();
    user.setFullName(request.fullName().trim());
    user.setPhoneNumber(trimToNull(request.phoneNumber()));
    user.setAddress(trimToNull(request.address()));
    user.setUpdatedAt(new Date());
    teacher.setSpecialization(trimToNull(request.specialization()));
    teacher.setDegree(trimToNull(request.degree()));
    teacher.setExperienceYears(request.experienceYears() == null ? 0 : request.experienceYears());
    return toProfile(teacherRepository.save(teacher));
  }

  private Teacher findCurrentTeacher(Principal principal) {
    return currentUserResolver.requireTeacher(principal);
  }

  private TeacherProfileResponse toProfile(Teacher teacher) {
    User user = teacher.getUserId();
    return new TeacherProfileResponse(
        teacher.getId(), teacher.getTeacherCode(), user.getId(), user.getUsername(),
        user.getFullName(), user.getEmail(), user.getPhoneNumber(), user.getAddress(),
        teacher.getSpecialization(), teacher.getDegree(), teacher.getExperienceYears(),
        user.getStatus().name(), user.getCreatedAt(), user.getUpdatedAt());
  }

}
