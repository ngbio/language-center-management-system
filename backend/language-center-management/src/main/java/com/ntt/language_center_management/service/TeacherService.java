package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import java.util.List;

public interface TeacherService {
  List<TeacherOptionResponse> getActiveTeachers();
}
