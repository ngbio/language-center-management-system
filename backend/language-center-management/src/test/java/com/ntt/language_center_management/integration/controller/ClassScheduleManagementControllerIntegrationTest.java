package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.admin.AdminCourseClassApiController;
import com.ntt.language_center_management.controller.management.ClassScheduleLessonApiController;
import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.service.ClassScheduleService;
import com.ntt.language_center_management.service.CourseClassService;
import com.ntt.language_center_management.service.LessonService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({AdminCourseClassApiController.class, ClassScheduleLessonApiController.class})
@Import(SecurityConfig.class)
class ClassScheduleManagementControllerIntegrationTest {

  private static final String COURSE_CLASS = """
      {"classCode":"EN-A1-01","className":"English A1 Morning",
       "startDate":"2027-01-10","endDate":"2027-03-10","maxStudents":20,
       "appliedTuitionFee":1200000,"courseId":1,"teacherId":2}
      """;
  private static final String IN_PERSON = """
      {"roomId":1,"dayOfWeek":2,"startTime":"08:00","endTime":"10:00",
       "deliveryMode":"IN_PERSON"}
      """;
  private static final String ONLINE = """
      {"dayOfWeek":4,"startTime":"19:00","endTime":"21:00",
       "deliveryMode":"ONLINE","meetingUrl":"https://meet.example/class"}
      """;

  @Autowired private MockMvc mockMvc;
  @MockitoBean private CourseClassService courseClassService;
  @MockitoBean private ClassScheduleService classScheduleService;
  @MockitoBean private LessonService lessonService;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCanLoadAllOwnedLessonsInOneRequest() throws Exception {
    when(lessonService.getMyLessons(any())).thenReturn(List.of());

    mockMvc.perform(get("/api/students/me/lessons")).andExpect(status().isOk());
    verify(lessonService).getMyLessons(any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void classCrudAndRequestValidationAreExposed() throws Exception {
    mockMvc.perform(get("/api/admin/classes")).andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/classes/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE_CLASS)).andExpect(status().isCreated());
    mockMvc.perform(put("/api/admin/classes/1").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE_CLASS)).andExpect(status().isOk());
    mockMvc.perform(delete("/api/admin/classes/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE_CLASS.replace("\"maxStudents\":20", "\"maxStudents\":0")))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void invalidCourseDatesFeeAndDuplicateCodeAreMapped() throws Exception {
    when(courseClassService.create(any(CourseClassRequest.class)))
        .thenThrow(new IllegalArgumentException("Ngày, học phí hoặc khóa học không hợp lệ"));
    mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE_CLASS)).andExpect(status().isBadRequest());

    when(courseClassService.create(any(CourseClassRequest.class)))
        .thenThrow(new DuplicateResourceException("Mã lớp đã tồn tại"));
    mockMvc.perform(post("/api/admin/classes").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE_CLASS)).andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void onlyActiveTeacherCanBeAssigned() throws Exception {
    mockMvc.perform(patch("/api/admin/classes/1/teacher")
        .contentType(MediaType.APPLICATION_JSON).content("{\"teacherId\":2}"))
        .andExpect(status().isOk());
    verify(courseClassService).assignTeacher(1, 2);

    when(courseClassService.assignTeacher(2, 3))
        .thenThrow(new IllegalArgumentException("Giảng viên chưa hoạt động"));
    mockMvc.perform(patch("/api/admin/classes/2/teacher")
        .contentType(MediaType.APPLICATION_JSON).content("{\"teacherId\":3}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void classStatusTransitionsAndOpenPreconditionsAreMapped() throws Exception {
    mockMvc.perform(patch("/api/admin/classes/1/status")
        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
        .andExpect(status().isOk());
    verify(courseClassService).changeStatus(1, ClassStatus.OPEN);

    when(courseClassService.changeStatus(2, ClassStatus.OPEN))
        .thenThrow(new IllegalArgumentException("Lớp cần giảng viên và lịch học hợp lệ"));
    mockMvc.perform(patch("/api/admin/classes/2/status")
        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"OPEN\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "CONSULTANT")
  void consultantCanReadClassesForEnrollmentButCannotManageSchedulesOrClassStatus() throws Exception {
    mockMvc.perform(get("/api/admin/classes")).andExpect(status().isOk());
    when(classScheduleService.getByClassId(1)).thenReturn(List.of());
    mockMvc.perform(get("/api/classes/1/schedules")).andExpect(status().isOk());
    mockMvc.perform(post("/api/classes/1/schedules").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isForbidden());
    mockMvc.perform(put("/api/schedules/1").contentType(MediaType.APPLICATION_JSON)
        .content(ONLINE)).andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/schedules/1")).andExpect(status().isForbidden());
    mockMvc.perform(patch("/api/admin/classes/1/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"OPEN\"}")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "CONSULTANT")
  void consultantCannotRescheduleLessons() throws Exception {
    mockMvc.perform(patch("/api/lessons/1/reschedule")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"lessonDate\":\"2027-02-15\",\"reason\":\"Đổi lịch\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void scheduleCannotBeUpdatedOrDeletedAfterLessonsExist() throws Exception {
    when(classScheduleService.update(eq(1), any(ClassScheduleRequest.class)))
        .thenThrow(new IllegalArgumentException("Không thể thay đổi lịch sau khi đã sinh buổi học"));
    mockMvc.perform(put("/api/schedules/1").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isBadRequest());
    doThrow(new IllegalArgumentException("Không thể xóa lịch đã sinh buổi học"))
        .when(classScheduleService).delete(1);
    mockMvc.perform(delete("/api/schedules/1")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void classTimeRoomAndTeacherConflictsAreReturnedAs409() throws Exception {
    when(classScheduleService.create(eq(1), any(ClassScheduleRequest.class)))
        .thenThrow(new DuplicateResourceException("Lịch mới bị chồng thời gian trong lớp"))
        .thenThrow(new DuplicateResourceException("Lịch học bị trùng phòng"))
        .thenThrow(new DuplicateResourceException("Lịch học bị trùng giảng viên"));
    for (int attempt = 0; attempt < 3; attempt++) {
      mockMvc.perform(post("/api/classes/1/schedules").contentType(MediaType.APPLICATION_JSON)
          .content(IN_PERSON)).andExpect(status().isConflict());
    }
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void scheduleUpdateUsesServiceConflictCheckThatExcludesItself() throws Exception {
    mockMvc.perform(put("/api/schedules/9").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isOk());
    verify(classScheduleService).update(eq(9), any(ClassScheduleRequest.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void cancelledClassDoesNotCauseResourceConflict() throws Exception {
    mockMvc.perform(post("/api/classes/3/schedules").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isCreated());
    verify(classScheduleService).create(eq(3), any(ClassScheduleRequest.class));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void deliveryModeRulesAreMappedTo400() throws Exception {
    when(classScheduleService.create(eq(1), any(ClassScheduleRequest.class)))
        .thenThrow(new IllegalArgumentException("Lịch trực tiếp phải chọn phòng"))
        .thenThrow(new IllegalArgumentException("Lịch online phải có đường dẫn phòng học"));
    mockMvc.perform(post("/api/classes/1/schedules").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isBadRequest());
    mockMvc.perform(post("/api/classes/1/schedules").contentType(MediaType.APPLICATION_JSON)
        .content(ONLINE)).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "TEACHER")
  void teacherCannotMutateSchedules() throws Exception {
    mockMvc.perform(post("/api/classes/1/schedules").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isForbidden());
    mockMvc.perform(put("/api/schedules/1").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isForbidden());
    mockMvc.perform(delete("/api/schedules/1")).andExpect(status().isForbidden());
  }

  @Test
  void anonymousCannotManageClassesOrSchedules() throws Exception {
    mockMvc.perform(get("/api/admin/classes")).andExpect(status().isUnauthorized());
    mockMvc.perform(post("/api/classes/1/schedules").contentType(MediaType.APPLICATION_JSON)
        .content(IN_PERSON)).andExpect(status().isUnauthorized());
  }
}
