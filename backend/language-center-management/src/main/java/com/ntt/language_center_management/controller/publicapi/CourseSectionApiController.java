package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.CourseContentResponse;
import com.ntt.language_center_management.service.CourseCurriculumService;
import java.util.List;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sections")
public class CourseSectionApiController {
  private final CourseCurriculumService courseCurriculumService;

  public CourseSectionApiController(CourseCurriculumService courseCurriculumService) {
    this.courseCurriculumService = courseCurriculumService;
  }

  @GetMapping("/{id}/contents")
  public ApiResponse<List<CourseContentResponse>> getContents(
      @PathVariable Integer id, Principal principal) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách nội dung của phần thành công",
        courseCurriculumService.getPublishedContents(id, principal));
  }
}
