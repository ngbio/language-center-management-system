package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import java.util.List;

public interface ClassScheduleService {

  List<ClassScheduleResponse> getByClassId(Integer classId);

  ClassScheduleResponse create(Integer classId, ClassScheduleRequest request);

  ClassScheduleResponse update(Integer id, ClassScheduleRequest request);

  void delete(Integer id);
}
