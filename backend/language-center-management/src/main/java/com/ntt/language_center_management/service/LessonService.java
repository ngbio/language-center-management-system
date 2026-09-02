package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.LessonRescheduleRequest;
import com.ntt.language_center_management.dto.request.LessonUpdateRequest;
import com.ntt.language_center_management.dto.response.LessonResponse;
import java.security.Principal;
import java.util.List;

public interface LessonService {

  List<LessonResponse> generate(Integer classId);

  List<LessonResponse> getByClassId(Integer classId, Principal principal);

  LessonResponse update(Integer id, LessonUpdateRequest request, Principal principal);

  LessonResponse reschedule(Integer id, LessonRescheduleRequest request);

  LessonResponse cancel(Integer id);
}
