package com.ntt.language_center_management.controller;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.service.CourseClassService;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
public class CourseClassApiController {

  private final CourseClassService courseClassService;

  public CourseClassApiController(CourseClassService courseClassService) {
    this.courseClassService = courseClassService;
  }

  @GetMapping
  public ApiResponse<PageResponse<CourseClassResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer courseId,
      @RequestParam(required = false) Integer levelId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "startDate") String sort,
      @RequestParam(defaultValue = "asc") String direction) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách lớp đang mở thành công",
        courseClassService.searchOpenClasses(
            keyword, courseId, levelId, date, page, size, sort, direction));
  }

  @GetMapping("/{id}")
  public ApiResponse<CourseClassResponse> get(@PathVariable Integer id) {
    return new ApiResponse<>(
        200, "Lấy thông tin lớp học thành công", courseClassService.getById(id));
  }
}
