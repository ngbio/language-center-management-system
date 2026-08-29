package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import com.ntt.language_center_management.entity.Teacher;
import org.springframework.stereotype.Component;

@Component
public class TeacherMapper {
  public TeacherOptionResponse toOptionResponse(Teacher teacher) {
    return new TeacherOptionResponse(
        teacher.getId(),
        teacher.getTeacherCode(),
        teacher.getUserId().getFullName(),
        teacher.getSpecialization(),
        teacher.getDegree());
  }
}
