package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.CourseRequest;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.PublicationStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.CourseMapper;
import com.ntt.language_center_management.repository.CourseRepository;
import com.ntt.language_center_management.repository.LevelRepository;
import com.ntt.language_center_management.service.impl.CourseServiceImpl;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

class CourseServiceImplTest {
  private CourseRepository courses;
  private LevelRepository levels;
  private CourseServiceImpl service;

  @BeforeEach
  void setUp() {
    courses = mock(CourseRepository.class);
    levels = mock(LevelRepository.class);
    service = new CourseServiceImpl(courses, levels, new CourseMapper());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldNormalizePageSortDirectionAndFiltersWhenSearching() {
    when(courses.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(Page.empty());
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

    var result = service.search("  English  ", 1, 2, " active ", -2, 500,
        "unknown", "desc");

    verify(courses).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    assertThat(pageable.getValue().getSort().getOrderFor("courseCode").isDescending()).isTrue();
    assertThat(result.content()).isEmpty();
  }

  @Test
  void shouldUseDefaultDirectionAndRejectUnsupportedStatusWhenSearching() {
    when(courses.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    service.search(null, null, null, null, 0, 20, "courseName", "sideways");
    verify(courses).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("courseName").isAscending()).isTrue();
    assertThatThrownBy(() -> service.search(null, null, null, "ARCHIVED", 0, 20,
        "courseCode", "asc")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRequireActivePublishedCourseWhenGettingBySlug() {
    Course course = course();
    when(courses.findBySlugAndStatusAndPublicationStatus(
        "english-a1", CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED))
        .thenReturn(Optional.of(course));

    assertThat(service.getPublishedBySlug(" English-A1 ").slug()).isEqualTo("english-a1");
    verify(courses).findBySlugAndStatusAndPublicationStatus(
        "english-a1", CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED);
  }

  @Test
  void shouldReturnResourceNotFoundWhenCourseOrSlugDoesNotExist() {
    when(courses.findById(99)).thenReturn(Optional.empty());
    when(courses.findBySlugAndStatusAndPublicationStatus(
        "missing", CatalogStatus.ACTIVE, PublicationStatus.PUBLISHED))
        .thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getById(99)).isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.getPublishedBySlug("missing"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldNormalizeCodeSlugAndOptionalTextWhenCreatingCourse() {
    stubActiveLevel();
    when(courses.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));
    CourseRequest request = request();
    request.setCourseCode(" en-a1 ");
    request.setSlug(" English-A1 ");
    request.setShortDescription("  Short  ");
    request.setDescription("   ");

    var result = service.save(request);

    assertThat(result.courseCode()).isEqualTo("EN-A1");
    assertThat(result.slug()).isEqualTo("english-a1");
    assertThat(result.shortDescription()).isEqualTo("Short");
    assertThat(result.description()).isNull();
    assertThat(result.createdAt()).isNotNull();
  }

  @Test
  void shouldRejectCourseWhenCodeOrSlugIsDuplicated() {
    CourseRequest request = request();
    when(courses.existsByCourseCodeIgnoreCase("EN-A1")).thenReturn(true);
    assertThatThrownBy(() -> service.save(request)).isInstanceOf(DuplicateResourceException.class);
    reset(courses);
    when(courses.existsBySlugIgnoreCase("english-a1")).thenReturn(true);
    assertThatThrownBy(() -> service.save(request)).isInstanceOf(DuplicateResourceException.class);
    verify(courses, never()).save(any());
  }

  @Test
  void shouldExcludeCurrentCourseFromDuplicateChecksWhenUpdating() {
    Course existing = course();
    CourseRequest request = request();
    request.setId(5);
    stubActiveLevel();
    when(courses.findById(5)).thenReturn(Optional.of(existing));
    when(courses.save(existing)).thenReturn(existing);

    var result = service.save(request);

    verify(courses).existsByCourseCodeIgnoreCaseAndIdNot("EN-A1", 5);
    verify(courses).existsBySlugIgnoreCaseAndIdNot("english-a1", 5);
    assertThat(result.id()).isEqualTo(5);
  }

  @Test
  void shouldSetOrClearPublishedAtWhenPublicationStatusChanges() {
    stubActiveLevel();
    when(courses.save(any(Course.class))).thenAnswer(i -> i.getArgument(0));
    CourseRequest published = request();
    published.setPublicationStatus(PublicationStatus.PUBLISHED);
    Course created = captureSavedCourseAfter(() -> service.save(published));
    assertThat(created.getPublishedAt()).isNotNull();

    reset(courses);
    Course existing = course();
    existing.setPublishedAt(new Date());
    CourseRequest draft = request();
    draft.setId(5);
    when(levels.findById(2)).thenReturn(Optional.of(activeLevel()));
    when(courses.findById(5)).thenReturn(Optional.of(existing));
    when(courses.save(existing)).thenReturn(existing);
    service.save(draft);
    assertThat(existing.getPublishedAt()).isNull();
  }

  @Test
  void shouldRejectNullCatalogOrPublicationStatus() {
    CourseRequest request = request();
    request.setStatus(null);
    assertThatThrownBy(() -> service.save(request)).isInstanceOf(IllegalArgumentException.class);
    request.setStatus(CatalogStatus.ACTIVE);
    request.setPublicationStatus(null);
    assertThatThrownBy(() -> service.save(request)).isInstanceOf(IllegalArgumentException.class);
    verify(courses, never()).save(any());
  }

  @Test
  void shouldRejectDeletionWhenCourseHasClassOrSection() {
    Course withClass = course();
    withClass.setCourseclassList(List.of(new Courseclass(1)));
    when(courses.findById(5)).thenReturn(Optional.of(withClass));
    assertThatThrownBy(() -> service.delete(5)).isInstanceOf(IllegalArgumentException.class);

    Course withSection = course();
    withSection.setCourseSectionList(List.of(new CourseSection(1)));
    when(courses.findById(6)).thenReturn(Optional.of(withSection));
    assertThatThrownBy(() -> service.delete(6)).isInstanceOf(IllegalArgumentException.class);
    verify(courses, never()).delete(any(Course.class));
  }

  private Course captureSavedCourseAfter(Runnable action) {
    ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
    action.run();
    verify(courses).save(captor.capture());
    return captor.getValue();
  }

  private void stubActiveLevel() {
    when(levels.findById(2)).thenReturn(Optional.of(activeLevel()));
  }

  private Level activeLevel() {
    Language language = new Language(1);
    language.setLanguageCode("EN"); language.setLanguageName("English");
    language.setStatus(CatalogStatus.ACTIVE);
    Level level = new Level(2);
    level.setLevelCode("A1"); level.setLevelName("Beginner");
    level.setStatus(CatalogStatus.ACTIVE); level.setLanguageId(language);
    return level;
  }

  private Course course() {
    Course course = new Course(5, "EN-A1", "English A1", "english-a1",
        new BigDecimal("1000000"), 20, CatalogStatus.ACTIVE,
        PublicationStatus.DRAFT, false, new Date());
    course.setLevelId(activeLevel());
    return course;
  }

  private CourseRequest request() {
    CourseRequest request = new CourseRequest();
    request.setCourseCode("EN-A1"); request.setCourseName(" English A1 ");
    request.setSlug("english-a1"); request.setTuitionFee(new BigDecimal("1000000"));
    request.setTotalSessions(20); request.setDurationHours(40); request.setLevelId(2);
    request.setStatus(CatalogStatus.ACTIVE);
    request.setPublicationStatus(PublicationStatus.DRAFT);
    return request;
  }
}
