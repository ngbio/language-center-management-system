package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import com.ntt.language_center_management.mapper.TeacherMapper;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.service.TeacherService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
