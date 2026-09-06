package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.TeacherProfileUpdateRequest;
import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import com.ntt.language_center_management.dto.response.TeacherProfileResponse;
import java.security.Principal;
import java.util.List;

public interface TeacherService {
  List<TeacherOptionResponse> getActiveTeachers();

  TeacherProfileResponse getProfile(Principal principal);

  TeacherProfileResponse updateProfile(Principal principal, TeacherProfileUpdateRequest request);
}
