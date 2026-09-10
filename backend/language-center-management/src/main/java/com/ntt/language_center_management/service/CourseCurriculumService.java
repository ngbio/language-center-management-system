package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.CourseContentResponse;
import com.ntt.language_center_management.dto.response.CourseSectionResponse;
import java.util.List;
import java.security.Principal;
import com.ntt.language_center_management.dto.request.CourseContentRequest;
import com.ntt.language_center_management.dto.request.CourseSectionRequest;
import com.ntt.language_center_management.dto.response.AdminCourseContentResponse;
import com.ntt.language_center_management.enums.PublicationStatus;

public interface CourseCurriculumService {

  List<CourseSectionResponse> getPublishedSections(Integer courseId);

  List<CourseContentResponse> getPublishedContents(Integer sectionId, Principal principal);

  List<CourseSectionResponse> getAdminSections(Integer courseId);
  CourseSectionResponse createSection(Integer courseId, CourseSectionRequest request);
  CourseSectionResponse updateSection(Integer id, CourseSectionRequest request);
  void deleteSection(Integer id);
  List<CourseSectionResponse> reorderSections(List<Integer> ids);
  List<AdminCourseContentResponse> getAdminContents(Integer sectionId);
  AdminCourseContentResponse createContent(Integer sectionId, CourseContentRequest request);
  AdminCourseContentResponse updateContent(Integer id, CourseContentRequest request);
  void deleteContent(Integer id);
  AdminCourseContentResponse changePublicationStatus(Integer id, PublicationStatus status);
  List<AdminCourseContentResponse> reorderContents(List<Integer> ids);
}
