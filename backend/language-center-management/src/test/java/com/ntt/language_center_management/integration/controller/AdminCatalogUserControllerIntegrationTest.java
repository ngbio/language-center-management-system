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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.admin.AdminCourseApiController;
import com.ntt.language_center_management.controller.admin.AdminLanguageApiController;
import com.ntt.language_center_management.controller.admin.AdminLevelApiController;
import com.ntt.language_center_management.controller.admin.AdminRoomApiController;
import com.ntt.language_center_management.controller.admin.ApiAdminUserController;
import com.ntt.language_center_management.controller.publicapi.LanguageApiController;
import com.ntt.language_center_management.dto.request.CourseRequest;
import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.dto.request.RoomRequest;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.service.CourseService;
import com.ntt.language_center_management.service.LanguageService;
import com.ntt.language_center_management.service.LevelService;
import com.ntt.language_center_management.service.RoomService;
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

@WebMvcTest({AdminLanguageApiController.class, AdminLevelApiController.class,
    AdminRoomApiController.class, AdminCourseApiController.class, ApiAdminUserController.class,
    LanguageApiController.class})
@Import(SecurityConfig.class)
class AdminCatalogUserControllerIntegrationTest {

  private static final String LANGUAGE = """
      {"languageCode":"EN","languageName":"English","status":"ACTIVE"}
      """;
  private static final String LEVEL = """
      {"languageId":1,"levelCode":"A1","levelName":"Beginner","displayOrder":1,"status":"ACTIVE"}
      """;
  private static final String ROOM = """
      {"roomCode":"R101","roomName":"Room 101","capacity":20,"status":"ACTIVE"}
      """;
  private static final String COURSE = """
      {"courseCode":"ENG-A1","courseName":"English A1","slug":"english-a1","tuitionFee":1000000,
       "totalSessions":20,"durationHours":40,"levelId":1,"status":"ACTIVE","publicationStatus":"DRAFT"}
      """;

  @Autowired private MockMvc mockMvc;
  @MockitoBean private LanguageService languageService;
  @MockitoBean private LevelService levelService;
  @MockitoBean private RoomService roomService;
  @MockitoBean private CourseService courseService;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  @WithMockUser(roles = "ADMIN")
  void languageCrudAndDependentLevelDeletionAreExposedCorrectly() throws Exception {
    when(languageService.deleteLanguage(1)).thenReturn(true);
    mockMvc.perform(get("/api/admin/languages")).andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/languages/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/languages").contentType(MediaType.APPLICATION_JSON)
        .content(LANGUAGE)).andExpect(status().isCreated());
    mockMvc.perform(put("/api/admin/languages/1").contentType(MediaType.APPLICATION_JSON)
        .content(LANGUAGE)).andExpect(status().isOk());
    mockMvc.perform(delete("/api/admin/languages/1")).andExpect(status().isOk());

    when(languageService.deleteLanguage(2)).thenReturn(false);
    mockMvc.perform(delete("/api/admin/languages/2")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void duplicateLanguageCodeOrNameReturns409() throws Exception {
    when(languageService.addOrUpdateLanguage(any(LanguageRequest.class)))
        .thenThrow(new DuplicateResourceException("Mã hoặc tên ngôn ngữ đã tồn tại"));
    mockMvc.perform(post("/api/admin/languages").contentType(MediaType.APPLICATION_JSON)
            .content(LANGUAGE))
        .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void languageStatusPatchAndPublicActiveQueryUseSeparateServiceContracts() throws Exception {
    mockMvc.perform(patch("/api/admin/languages/1/status")
            .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"INACTIVE\"}"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/languages")).andExpect(status().isOk());
    verify(languageService).changeStatus(1, CatalogStatus.INACTIVE);
    verify(languageService).getActiveLanguages();
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void levelCrudDuplicateRulesInactiveLanguageAndCourseDependencyAreMapped() throws Exception {
    mockMvc.perform(get("/api/admin/levels").param("languageId", "1"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/levels/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/levels").contentType(MediaType.APPLICATION_JSON)
        .content(LEVEL)).andExpect(status().isCreated());
    mockMvc.perform(put("/api/admin/levels/1").contentType(MediaType.APPLICATION_JSON)
        .content(LEVEL)).andExpect(status().isOk());
    mockMvc.perform(delete("/api/admin/levels/1")).andExpect(status().isOk());

    when(levelService.save(any(LevelRequest.class)))
        .thenThrow(new DuplicateResourceException("Mã hoặc thứ tự đã tồn tại"));
    mockMvc.perform(post("/api/admin/levels").contentType(MediaType.APPLICATION_JSON)
        .content(LEVEL)).andExpect(status().isConflict());
    when(levelService.save(any(LevelRequest.class)))
        .thenThrow(new IllegalArgumentException("Chỉ được tạo cho ngôn ngữ hoạt động"));
    mockMvc.perform(post("/api/admin/levels").contentType(MediaType.APPLICATION_JSON)
        .content(LEVEL)).andExpect(status().isBadRequest());
    doThrow(new IllegalArgumentException("Không thể xóa trình độ đã có khóa học"))
        .when(levelService).delete(2);
    mockMvc.perform(delete("/api/admin/levels/2")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void roomCrudDuplicateCapacityValidationAndScheduleDependencyAreMapped() throws Exception {
    mockMvc.perform(get("/api/admin/rooms")).andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/rooms/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/rooms").contentType(MediaType.APPLICATION_JSON)
        .content(ROOM)).andExpect(status().isCreated());
    mockMvc.perform(put("/api/admin/rooms/1").contentType(MediaType.APPLICATION_JSON)
        .content(ROOM)).andExpect(status().isOk());
    mockMvc.perform(delete("/api/admin/rooms/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/rooms").contentType(MediaType.APPLICATION_JSON)
        .content("{\"roomCode\":\"R1\",\"roomName\":\"Room\",\"capacity\":0,\"status\":\"ACTIVE\"}"))
        .andExpect(status().isBadRequest());

    when(roomService.save(any(RoomRequest.class)))
        .thenThrow(new DuplicateResourceException("Mã phòng đã tồn tại"));
    mockMvc.perform(post("/api/admin/rooms").contentType(MediaType.APPLICATION_JSON)
        .content(ROOM)).andExpect(status().isConflict());
    doThrow(new IllegalArgumentException("Không thể xóa phòng đã có lịch học"))
        .when(roomService).delete(2);
    mockMvc.perform(delete("/api/admin/rooms/2")).andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void courseCrudSearchPublicationValidationAndDuplicateRulesAreMapped() throws Exception {
    when(courseService.search(any(), any(), any(), any(), eq(0), eq(10), eq("courseCode"), eq("asc")))
        .thenReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, true, true));
    mockMvc.perform(get("/api/admin/courses").param("status", "ACTIVE"))
        .andExpect(status().isOk());
    mockMvc.perform(get("/api/admin/courses/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/courses").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE)).andExpect(status().isCreated());
    mockMvc.perform(put("/api/admin/courses/1").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE)).andExpect(status().isOk());
    mockMvc.perform(delete("/api/admin/courses/1")).andExpect(status().isOk());
    mockMvc.perform(post("/api/admin/courses").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE.replace("english-a1", "English A1"))).andExpect(status().isBadRequest());

    when(courseService.save(any(CourseRequest.class)))
        .thenThrow(new DuplicateResourceException("Mã hoặc slug khóa học đã tồn tại"));
    mockMvc.perform(post("/api/admin/courses").contentType(MediaType.APPLICATION_JSON)
        .content(COURSE)).andExpect(status().isConflict());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void usersSupportSearchFiltersPaginationDetailAndStatusChanges() throws Exception {
    mockMvc.perform(get("/api/admin/users").param("keyword", "an")
            .param("roleCode", "TEACHER").param("status", "PENDING")
            .param("page", "1").param("size", "20").param("sort", "fullName")
            .param("direction", "asc"))
        .andExpect(status().isOk());
    verify(userService).searchUsers("an", "TEACHER", "PENDING", 1, 20, "fullName", "asc");
    mockMvc.perform(get("/api/admin/users/7")).andExpect(status().isOk());
    verify(userService).getUserById(7);

    mockMvc.perform(patch("/api/admin/users/7/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ACTIVE\"}")).andExpect(status().isOk());
    mockMvc.perform(patch("/api/admin/users/8/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"INACTIVE\"}")).andExpect(status().isOk());
    verify(userService).changeStatus(7, AccountStatus.ACTIVE);
    verify(userService).changeStatus(8, AccountStatus.INACTIVE);
  }

  @Test
  void anonymousCannotAccessAdminEndpoints() throws Exception {
    mockMvc.perform(get("/api/admin/languages")).andExpect(status().isUnauthorized());
    mockMvc.perform(get("/api/admin/users")).andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockUser(roles = "STUDENT")
  void studentCannotAccessAdminEndpoints() throws Exception {
    mockMvc.perform(get("/api/admin/languages")).andExpect(status().isForbidden());
    mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
  }

  @Test
  @WithMockUser(roles = "TEACHER")
  void teacherCannotAccessAdminEndpoints() throws Exception {
    mockMvc.perform(get("/api/admin/courses")).andExpect(status().isForbidden());
    mockMvc.perform(patch("/api/admin/users/1/status").contentType(MediaType.APPLICATION_JSON)
        .content("{\"status\":\"ACTIVE\"}")).andExpect(status().isForbidden());
  }
}
