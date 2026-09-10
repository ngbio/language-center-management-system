package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.dto.response.CourseResponse;
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

  CourseClassResponse getAdminById(Integer id);

  CourseClassResponse create(CourseClassRequest request);

  CourseClassResponse update(Integer id, CourseClassRequest request);

  void deleteDraft(Integer id);

  CourseClassResponse assignTeacher(Integer id, Integer teacherId);

  CourseClassResponse changeStatus(Integer id, ClassStatus status);

  List<CourseClassResponse> getTeacherClasses(Principal principal);

  List<CourseResponse> getTeacherCourses(Principal principal);
}
