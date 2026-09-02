package com.ntt.language_center_management.controller.publicapi;

import com.ntt.language_center_management.dto.request.CourseRequest;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses")
public class CourseApiController {
  private final CourseService courseService;

  public CourseApiController(CourseService courseService) {
    this.courseService = courseService;
  }

  @GetMapping
  public ApiResponse<PageResponse<CourseResponse>> search(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Integer languageId,
      @RequestParam(required = false) Integer levelId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "courseCode") String sort) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách khóa học thành công",
        courseService.search(keyword, languageId, levelId, status, page, size, sort));
  }

  @GetMapping("/{id}")
  public ApiResponse<CourseResponse> get(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Lấy khóa học thành công", courseService.getById(id));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CourseResponse>> create(
      @Valid @RequestBody CourseRequest request) {
    request.setId(null);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(new ApiResponse<>(201, "Tạo khóa học thành công", courseService.save(request)));
  }

  @PutMapping("/{id}")
  public ApiResponse<CourseResponse> update(
      @PathVariable Integer id, @Valid @RequestBody CourseRequest request) {
    request.setId(id);
    return new ApiResponse<>(200, "Cập nhật khóa học thành công", courseService.save(request));
  }

  @DeleteMapping("/{id}")
  public ApiResponse<Void> delete(@PathVariable Integer id) {
    courseService.delete(id);
    return new ApiResponse<>(200, "Xóa khóa học thành công", null);
  }
}
