package com.ntt.language_center_management.controller.management;

import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.dto.request.LessonRescheduleRequest;
import com.ntt.language_center_management.dto.request.LessonUpdateRequest;
import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.dto.response.LessonResponse;
import com.ntt.language_center_management.service.ClassScheduleService;
import com.ntt.language_center_management.service.LessonService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ClassScheduleLessonApiController {
  private final ClassScheduleService classScheduleService;
  private final LessonService lessonService;

  public ClassScheduleLessonApiController(
      ClassScheduleService classScheduleService, LessonService lessonService) {
    this.classScheduleService = classScheduleService;
    this.lessonService = lessonService;
  }

  @GetMapping("/classes/{classId}/schedules")
  public ApiResponse<List<ClassScheduleResponse>> getSchedules(
      @PathVariable Integer classId, Authentication authentication) {
    boolean canManageSchedules =
        authentication != null
            && authentication.getAuthorities().stream()
                .anyMatch(
                    authority ->
                        "ROLE_ADMIN".equals(authority.getAuthority()));
    List<ClassScheduleResponse> visibleSchedules =
        classScheduleService.getByClassId(classId).stream()
            .map(schedule -> canManageSchedules ? schedule : withoutMeetingUrl(schedule))
            .toList();
    return new ApiResponse<>(
        200, "Lấy lịch học thành công", visibleSchedules);
  }

  private ClassScheduleResponse withoutMeetingUrl(ClassScheduleResponse schedule) {
    return new ClassScheduleResponse(
        schedule.id(), schedule.courseClassId(), schedule.classCode(), schedule.className(),
        schedule.dayOfWeek(), schedule.startTime(), schedule.endTime(), schedule.deliveryMode(),
        schedule.roomId(), schedule.roomCode(), schedule.roomName(), schedule.roomLocation(), null);
  }

  @PostMapping("/classes/{classId}/schedules")
  public ResponseEntity<ApiResponse<ClassScheduleResponse>> createSchedule(
      @PathVariable Integer classId, @Valid @RequestBody ClassScheduleRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new ApiResponse<>(
                201,
                "Thêm lịch học thành công",
                classScheduleService.create(classId, request)));
  }

  @PutMapping("/schedules/{id}")
  public ApiResponse<ClassScheduleResponse> updateSchedule(
      @PathVariable Integer id, @Valid @RequestBody ClassScheduleRequest request) {
    return new ApiResponse<>(
        200, "Cập nhật lịch học thành công", classScheduleService.update(id, request));
  }

  @DeleteMapping("/schedules/{id}")
  public ApiResponse<Void> deleteSchedule(@PathVariable Integer id) {
    classScheduleService.delete(id);
    return new ApiResponse<>(200, "Xóa lịch học thành công", null);
  }

  @PostMapping("/classes/{classId}/lessons/generate")
  public ResponseEntity<ApiResponse<List<LessonResponse>>> generateLessons(
      @PathVariable Integer classId, Principal principal) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            new ApiResponse<>(
                201,
                "Sinh danh sách buổi học thành công",
                lessonService.generate(classId, principal)));
  }

  @GetMapping("/classes/{classId}/lessons")
  public ApiResponse<List<LessonResponse>> getLessons(
      @PathVariable Integer classId, Principal principal) {
    return new ApiResponse<>(
        200, "Lấy danh sách buổi học thành công", lessonService.getByClassId(classId, principal));
  }

  @GetMapping("/students/me/lessons")
  public ApiResponse<List<LessonResponse>> getMyLessons(Principal principal) {
    return new ApiResponse<>(
        200, "Lấy danh sách buổi học của tôi thành công", lessonService.getMyLessons(principal));
  }

  @PutMapping("/lessons/{id}")
  public ApiResponse<LessonResponse> updateLesson(
      @PathVariable Integer id,
      @Valid @RequestBody LessonUpdateRequest request,
      Principal principal) {
    return new ApiResponse<>(
        200, "Cập nhật nội dung buổi học thành công", lessonService.update(id, request, principal));
  }

  @PatchMapping("/lessons/{id}/reschedule")
  public ApiResponse<LessonResponse> rescheduleLesson(
      @PathVariable Integer id,
      @Valid @RequestBody LessonRescheduleRequest request) {
    return new ApiResponse<>(
        200, "Dời buổi học thành công", lessonService.reschedule(id, request));
  }

  @PatchMapping("/lessons/{id}/cancel")
  public ApiResponse<LessonResponse> cancelLesson(@PathVariable Integer id) {
    return new ApiResponse<>(200, "Hủy buổi học thành công", lessonService.cancel(id));
  }
}
