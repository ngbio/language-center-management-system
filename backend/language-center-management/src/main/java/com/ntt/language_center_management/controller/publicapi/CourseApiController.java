package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.CourseSectionResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.service.CourseService;
import com.ntt.language_center_management.service.CourseCurriculumService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseApiController {
  private final CourseService courseService;
  private final CourseCurriculumService courseCurriculumService;

  public CourseApiController(
      CourseService courseService, CourseCurriculumService courseCurriculumService) {
    this.courseService = courseService;
    this.courseCurriculumService = courseCurriculumService;
  }

  @GetMapping
  public ApiResponse<PageResponse<CourseResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer languageId,
      @RequestParam(required = false) Integer levelId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "courseCode") String sort) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách khóa học thành công",
        courseService.searchPublished(keyword, languageId, levelId, page, size, sort));
  }

  @GetMapping("/{id}")
  public ApiResponse<CourseResponse> get(@PathVariable Integer id) {
    return new ApiResponse<>(
        200, "Lấy khóa học thành công", courseService.getPublishedById(id));
  }

  @GetMapping("/slug/{slug}")
  public ApiResponse<CourseResponse> getBySlug(@PathVariable String slug) {
    return new ApiResponse<>(
        200, "Lấy chi tiết khóa học thành công", courseService.getPublishedBySlug(slug));
  }

  @GetMapping("/{id}/sections")
  public ApiResponse<List<CourseSectionResponse>> getSections(@PathVariable Integer id) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách phần của khóa học thành công",
        courseCurriculumService.getPublishedSections(id));
  }
}
