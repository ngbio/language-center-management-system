package com.ntt.language_center_management.integration.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ntt.language_center_management.config.SecurityConfig;
import com.ntt.language_center_management.controller.publicapi.CourseApiController;
import com.ntt.language_center_management.controller.publicapi.CourseClassApiController;
import com.ntt.language_center_management.controller.publicapi.CourseSectionApiController;
import com.ntt.language_center_management.controller.publicapi.LanguageApiController;
import com.ntt.language_center_management.controller.publicapi.LevelApiController;
import com.ntt.language_center_management.controller.publicapi.RoomApiController;
import com.ntt.language_center_management.controller.publicapi.TeacherApiController;
import com.ntt.language_center_management.dto.response.CourseContentResponse;
import com.ntt.language_center_management.dto.response.CourseSectionResponse;
import com.ntt.language_center_management.dto.response.LanguageResponse;
import com.ntt.language_center_management.dto.response.LevelResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.dto.response.RoomResponse;
import com.ntt.language_center_management.dto.response.TeacherOptionResponse;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.CourseContentType;
import com.ntt.language_center_management.enums.RoomStatus;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.service.CourseClassService;
import com.ntt.language_center_management.service.CourseCurriculumService;
import com.ntt.language_center_management.service.CourseService;
import com.ntt.language_center_management.service.LanguageService;
import com.ntt.language_center_management.service.LevelService;
import com.ntt.language_center_management.service.RoomService;
import com.ntt.language_center_management.service.TeacherService;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({LanguageApiController.class, LevelApiController.class, CourseApiController.class,
    CourseClassApiController.class, CourseSectionApiController.class, TeacherApiController.class,
    RoomApiController.class})
@Import(SecurityConfig.class)
class PublicCatalogControllerIntegrationTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private LanguageService languageService;
  @MockitoBean private LevelService levelService;
  @MockitoBean private CourseService courseService;
  @MockitoBean private CourseCurriculumService courseCurriculumService;
  @MockitoBean private CourseClassService courseClassService;
  @MockitoBean private TeacherService teacherService;
  @MockitoBean private RoomService roomService;
  @MockitoBean private UserService userService;
  @MockitoBean private JwtUtils jwtUtils;

  @Test
  void languagesArePublicAndUseActiveCatalogQuery() throws Exception {
    when(languageService.getActiveLanguages()).thenReturn(List.of(
        new LanguageResponse(1, "EN", "English", null, CatalogStatus.ACTIVE)));
    mockMvc.perform(get("/api/languages")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].languageCode").value("EN"))
        .andExpect(jsonPath("$.data[0].status").value("ACTIVE"));
    verify(languageService).getActiveLanguages();
  }

  @Test
  void languageDetailUsesActiveLookupAndMapsMissingIdTo404() throws Exception {
    when(languageService.getActiveById(99)).thenThrow(new ResourceNotFoundException("Not found"));
    mockMvc.perform(get("/api/languages/99")).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    verify(languageService).getActiveById(99);
  }

  @Test
  void languageLevelsUseActiveQueryForSelectedLanguage() throws Exception {
    when(levelService.getActive(1)).thenReturn(List.of(level()));
    mockMvc.perform(get("/api/languages/1/levels")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].levelCode").value("A1"));
    verify(levelService).getActive(1);
  }

  @Test
  void levelsCanBeListedWithAndWithoutLanguageFilter() throws Exception {
    when(levelService.getActive(null)).thenReturn(List.of(level()));
    when(levelService.getActive(2)).thenReturn(List.of());
    mockMvc.perform(get("/api/levels")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].languageId").value(1));
    mockMvc.perform(get("/api/levels").param("languageId", "2")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray());
    verify(levelService).getActive(null);
    verify(levelService).getActive(2);
  }

  @Test
  void levelDetailUsesActiveLookupAndMapsMissingIdTo404() throws Exception {
    when(levelService.getActiveById(99)).thenThrow(new ResourceNotFoundException("Not found"));
    mockMvc.perform(get("/api/levels/99")).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void courseSearchForwardsAllFiltersSortingAndPagination() throws Exception {
    when(courseService.searchPublished("english", 1, 2, 3, 5, "courseName", "desc"))
        .thenReturn(emptyPage(3, 5));
    mockMvc.perform(get("/api/courses").param("keyword", "english")
            .param("languageId", "1").param("levelId", "2").param("page", "3")
            .param("size", "5").param("sort", "courseName").param("direction", "desc"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(3))
        .andExpect(jsonPath("$.data.size").value(5));
    verify(courseService).searchPublished("english", 1, 2, 3, 5, "courseName", "desc");
  }

  @Test
  void publishedCourseSlugMapsMissingCourseTo404() throws Exception {
    when(courseService.getPublishedBySlug("missing"))
        .thenThrow(new ResourceNotFoundException("Not found"));
    mockMvc.perform(get("/api/courses/slug/missing")).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
    verify(courseService).getPublishedBySlug("missing");
  }

  @Test
  void publishedCourseSectionsArePublic() throws Exception {
    when(courseCurriculumService.getPublishedSections(5))
        .thenReturn(List.of(new CourseSectionResponse(8, "Section 1", null, 1)));
    mockMvc.perform(get("/api/courses/5/sections")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].id").value(8));
  }

  @Test
  void anonymousCurriculumPassesNoPrincipalForPreviewEvaluation() throws Exception {
    when(courseCurriculumService.getPublishedContents(8, null)).thenReturn(List.of(content(true)));
    mockMvc.perform(get("/api/sections/8/contents")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].preview").value(true));
    verify(courseCurriculumService).getPublishedContents(8, null);
  }

  @Test
  @WithMockUser(username = "student@example.com", roles = "STUDENT")
  void authenticatedCurriculumPassesPrincipalForEnrollmentEvaluation() throws Exception {
    when(courseCurriculumService.getPublishedContents(any(Integer.class), any(Principal.class)))
        .thenReturn(List.of(content(false)));
    mockMvc.perform(get("/api/sections/8/contents")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].preview").value(false));
    verify(courseCurriculumService).getPublishedContents(any(Integer.class), any(Principal.class));
  }

  @Test
  void classSearchForwardsFiltersSortingAndPagination() throws Exception {
    when(courseClassService.searchOpenClasses(any(), any(), any(), any(Date.class), any(Integer.class),
        any(Integer.class), any(), any())).thenReturn(emptyPage(1, 6));
    mockMvc.perform(get("/api/classes").param("keyword", "evening").param("courseId", "4")
            .param("levelId", "2").param("date", "2026-09-20").param("page", "1")
            .param("size", "6").param("sort", "className").param("direction", "desc"))
        .andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(1));
    verify(courseClassService).searchOpenClasses(
        org.mockito.ArgumentMatchers.eq("evening"), org.mockito.ArgumentMatchers.eq(4),
        org.mockito.ArgumentMatchers.eq(2), any(Date.class), org.mockito.ArgumentMatchers.eq(1),
        org.mockito.ArgumentMatchers.eq(6), org.mockito.ArgumentMatchers.eq("className"),
        org.mockito.ArgumentMatchers.eq("desc"));
  }

  @Test
  void malformedClassDateReturns400() throws Exception {
    mockMvc.perform(get("/api/classes").param("date", "not-a-date"))
        .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
  }

  @Test
  void publicClassDetailMapsMissingClassTo404() throws Exception {
    when(courseClassService.getById(99)).thenThrow(new ResourceNotFoundException("Not found"));
    mockMvc.perform(get("/api/classes/99")).andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void teacherCatalogUsesActiveTeacherQuery() throws Exception {
    when(teacherService.getActiveTeachers()).thenReturn(List.of(
        new TeacherOptionResponse(1, "GV001", "Teacher One", "English", "MA")));
    mockMvc.perform(get("/api/teachers")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].teacherCode").value("GV001"));
    verify(teacherService).getActiveTeachers();
  }

  @Test
  void roomCatalogDoesNotExposeMeetingUrlOrAdministrativeFields() throws Exception {
    when(roomService.getAll()).thenReturn(List.of(
        new RoomResponse(1, "P101", "Room 101", 30, "Floor 1", RoomStatus.ACTIVE)));
    mockMvc.perform(get("/api/rooms")).andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].roomCode").value("P101"))
        .andExpect(jsonPath("$.data[0].meetingUrl").doesNotExist())
        .andExpect(jsonPath("$.data[0].createdAt").doesNotExist());
  }

  private LevelResponse level() {
    return new LevelResponse(10, "A1", "English A1", null, 1, CatalogStatus.ACTIVE,
        1, "EN", "English");
  }

  private CourseContentResponse content(boolean preview) {
    return new CourseContentResponse(1, "Lesson", "Summary", "<p>Content</p>", null, null,
        null, CourseContentType.LESSON, 1, preview);
  }

  private <T> PageResponse<T> emptyPage(int page, int size) {
    return new PageResponse<>(List.of(), page, size, 0, 0, true, true);
  }
}
