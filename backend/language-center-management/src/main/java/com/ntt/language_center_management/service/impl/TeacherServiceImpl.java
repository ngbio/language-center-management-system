package com.ntt.language_center_management.service.impl;

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
import java.security.Principal;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TeacherServiceImpl implements TeacherService {
  private final TeacherRepository teacherRepository;
  private final TeacherMapper teacherMapper;

  public TeacherServiceImpl(TeacherRepository teacherRepository, TeacherMapper teacherMapper) {
    this.teacherRepository = teacherRepository;
    this.teacherMapper = teacherMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<TeacherOptionResponse> getActiveTeachers() {
    return teacherRepository.findByUserStatus("ACTIVE").stream()
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
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new UnauthorizedException("Không thể xác định giảng viên hiện tại");
    }
    return teacherRepository.findByUserId_EmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hồ sơ giảng viên"));
  }

  private TeacherProfileResponse toProfile(Teacher teacher) {
    User user = teacher.getUserId();
    return new TeacherProfileResponse(
        teacher.getId(), teacher.getTeacherCode(), user.getId(), user.getUsername(),
        user.getFullName(), user.getEmail(), user.getPhoneNumber(), user.getAddress(),
        teacher.getSpecialization(), teacher.getDegree(), teacher.getExperienceYears(),
        user.getStatus(), user.getCreatedAt(), user.getUpdatedAt());
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
