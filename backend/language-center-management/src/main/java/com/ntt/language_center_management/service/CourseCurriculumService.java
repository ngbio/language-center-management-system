package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.CourseContentResponse;
import com.ntt.language_center_management.dto.response.CourseSectionResponse;
import java.util.List;
import java.security.Principal;

public interface CourseCurriculumService {

  List<CourseSectionResponse> getPublishedSections(Integer courseId);

  List<CourseContentResponse> getPublishedContents(Integer sectionId, Principal principal);
}
