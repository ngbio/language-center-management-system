package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import java.security.Principal;
import java.util.Date;
import java.util.List;

public interface CourseClassService {

  PageResponse<CourseClassResponse> searchOpenClasses(
      String keyword,
      Integer courseId,
      Integer levelId,
      Date date,
      int page,
      int size,
      String sort,
      String direction);

  PageResponse<CourseClassResponse> searchAdminClasses(
      String keyword,
      Integer courseId,
      Integer levelId,
      String status,
      int page,
      int size,
      String sort,
      String direction);

  CourseClassResponse getById(Integer id);

  CourseClassResponse create(CourseClassRequest request);

  CourseClassResponse update(Integer id, CourseClassRequest request);

  CourseClassResponse assignTeacher(Integer id, Integer teacherId);

  CourseClassResponse changeStatus(Integer id, String status);

  List<CourseClassResponse> getTeacherClasses(Principal principal);
}
