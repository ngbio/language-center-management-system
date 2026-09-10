package com.ntt.language_center_management.controller.admin;

import com.ntt.language_center_management.dto.request.*;
import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.CourseCurriculumService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminCourseCurriculumApiController {
  private final CourseCurriculumService service;
  public AdminCourseCurriculumApiController(CourseCurriculumService service) { this.service = service; }

  @GetMapping("/courses/{courseId}/sections")
  public ApiResponse<List<CourseSectionResponse>> sections(@PathVariable Integer courseId) {
    return ok("Lấy giáo trình thành công", service.getAdminSections(courseId));
  }

  @PostMapping("/courses/{courseId}/sections")
  public ResponseEntity<ApiResponse<CourseSectionResponse>> createSection(
      @PathVariable Integer courseId, @Valid @RequestBody CourseSectionRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
        new ApiResponse<>(201, "Tạo phần nội dung thành công", service.createSection(courseId, request)));
  }

  @PutMapping("/sections/{id}")
  public ApiResponse<CourseSectionResponse> updateSection(
      @PathVariable Integer id, @Valid @RequestBody CourseSectionRequest request) {
    return ok("Cập nhật phần nội dung thành công", service.updateSection(id, request));
  }

  @DeleteMapping("/sections/{id}")
  public ApiResponse<Void> deleteSection(@PathVariable Integer id) {
    service.deleteSection(id); return ok("Xóa phần nội dung thành công", null);
  }

  @PatchMapping("/sections/reorder")
  public ApiResponse<List<CourseSectionResponse>> reorderSections(@Valid @RequestBody ReorderRequest request) {
    return ok("Sắp xếp phần nội dung thành công", service.reorderSections(request.ids()));
  }

  @GetMapping("/sections/{sectionId}/contents")
  public ApiResponse<List<AdminCourseContentResponse>> contents(@PathVariable Integer sectionId) {
    return ok("Lấy nội dung giáo trình thành công", service.getAdminContents(sectionId));
  }

  @PostMapping("/sections/{sectionId}/contents")
  public ResponseEntity<ApiResponse<AdminCourseContentResponse>> createContent(
      @PathVariable Integer sectionId, @Valid @RequestBody CourseContentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
        new ApiResponse<>(201, "Tạo nội dung thành công", service.createContent(sectionId, request)));
  }

  @PutMapping("/contents/{id}")
  public ApiResponse<AdminCourseContentResponse> updateContent(
      @PathVariable Integer id, @Valid @RequestBody CourseContentRequest request) {
    return ok("Cập nhật nội dung thành công", service.updateContent(id, request));
  }

  @DeleteMapping("/contents/{id}")
  public ApiResponse<Void> deleteContent(@PathVariable Integer id) {
    service.deleteContent(id); return ok("Xóa nội dung thành công", null);
  }

  @PatchMapping("/contents/{id}/publication-status")
  public ApiResponse<AdminCourseContentResponse> publicationStatus(
      @PathVariable Integer id, @Valid @RequestBody PublicationStatusRequest request) {
    return ok("Cập nhật trạng thái xuất bản thành công",
        service.changePublicationStatus(id, request.status()));
  }

  @PatchMapping("/contents/reorder")
  public ApiResponse<List<AdminCourseContentResponse>> reorderContents(@Valid @RequestBody ReorderRequest request) {
    return ok("Sắp xếp nội dung thành công", service.reorderContents(request.ids()));
  }

  private <T> ApiResponse<T> ok(String message, T data) { return new ApiResponse<>(200, message, data); }
}
