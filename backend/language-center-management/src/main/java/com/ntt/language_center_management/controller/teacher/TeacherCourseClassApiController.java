package com.ntt.language_center_management.controller.teacher;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.service.CourseClassService;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teachers/me")
public class TeacherCourseClassApiController {

  private final CourseClassService courseClassService;

  public TeacherCourseClassApiController(CourseClassService courseClassService) {
    this.courseClassService = courseClassService;
  }

  @GetMapping("/classes")
  public ApiResponse<List<CourseClassResponse>> getMyClasses(Principal principal) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách lớp được phân công thành công",
        courseClassService.getTeacherClasses(principal));
  }

  @GetMapping("/courses")
  public ApiResponse<List<CourseResponse>> getMyCourses(Principal principal) {
    return new ApiResponse<>(
        200,
        "Lấy danh sách khóa học được phân công thành công",
        courseClassService.getTeacherCourses(principal));
  }
}
