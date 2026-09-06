package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.CourseRequest;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.PageResponse;

public interface CourseService {
  PageResponse<CourseResponse> search(
      String keyword,
      Integer languageId,
      Integer levelId,
      String status,
      int page,
      int size,
      String sort,
      String direction);

  CourseResponse getById(Integer id);

  CourseResponse getPublishedBySlug(String slug);

  PageResponse<CourseResponse> searchPublished(
      String keyword, Integer languageId, Integer levelId, int page, int size, String sort,
      String direction);

  CourseRequest getRequestById(Integer id);

  CourseResponse save(CourseRequest request);

  void delete(Integer id);
}
