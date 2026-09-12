package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.mapper.CourseClassMapper;
import com.ntt.language_center_management.mapper.CourseMapper;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.repository.projection.CourseClassEnrollmentCount;
import com.ntt.language_center_management.service.impl.CourseClassServiceImpl;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.event.ClassOpenedMailEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.ApplicationEventPublisher;

class CourseClassServiceImplTest {
  private CourseClassRepository classes;
  private CourseRepository courses;
  private TeacherRepository teachers;
  private EnrollmentRepository enrollments;
  private ClassScheduleRepository schedules;
  private CourseClassMapper mapper;
  private CourseMapper courseMapper;
  private ClassScheduleMapper scheduleMapper;
  private CourseClassServiceImpl service;
  private CurrentUserResolver currentUserResolver;
  private ApplicationEventPublisher eventPublisher;

  @BeforeEach
  void setUp() {
    classes = mock(CourseClassRepository.class); courses = mock(CourseRepository.class);
    teachers = mock(TeacherRepository.class); enrollments = mock(EnrollmentRepository.class);
    schedules = mock(ClassScheduleRepository.class); mapper = mock(CourseClassMapper.class);
    courseMapper = mock(CourseMapper.class);
    scheduleMapper = mock(ClassScheduleMapper.class);
    currentUserResolver = new CurrentUserResolver(
        mock(UserRepository.class), mock(StudentRepository.class), teachers);
    eventPublisher = mock(ApplicationEventPublisher.class);
    service = new CourseClassServiceImpl(classes, courses, teachers, enrollments, schedules,
        mapper, courseMapper, scheduleMapper, "Asia/Ho_Chi_Minh", currentUserResolver,
        eventPublisher);
  }

  @Test
  void shouldReturnMappedPageWithSafeParametersWhenSearchingPublicClasses() {
    Courseclass value = courseClass(ClassStatus.OPEN);
    CourseClassResponse response = mock(CourseClassResponse.class);
    when(classes.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(value)));
    CourseClassEnrollmentCount count = mock(CourseClassEnrollmentCount.class);
    when(count.getCourseClassId()).thenReturn(1);
    when(count.getEnrollmentCount()).thenReturn(2L);
    when(enrollments.countByCourseClassIdsAndEnrollmentStatusIn(anyList(), any()))
        .thenReturn(List.of(count));
    when(mapper.toResponse(eq(value), eq(2L), anyList())).thenReturn(response);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

    var result = service.searchOpenClasses(" english ", 3, 2, null, -1, 500,
        "invalid", "desc");

    verify(classes).findAll(any(Specification.class), pageable.capture());
    assertThat(result.content()).containsExactly(response);
    assertThat(pageable.getValue().getPageNumber()).isZero();
    assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
    assertThat(pageable.getValue().getSort().getOrderFor("startDate").isDescending()).isTrue();
  }

  @Test
  void shouldNormalizeStatusSortAndDirectionWhenSearchingAdminClasses() {
    when(classes.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

    service.searchAdminClasses(" class ", 3, 2, " open ", 0, 20,
        "className", "unexpected");

    verify(classes).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getSort().getOrderFor("className").isAscending()).isTrue();
    assertThatThrownBy(() -> service.searchAdminClasses(null, null, null, "BROKEN", 0, 20,
        "startDate", "asc")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCreateDraftClassWhenRequestIsValid() {
    CourseClassRequest request = request();
    when(courses.findByIdAndStatus(3, CatalogStatus.ACTIVE)).thenReturn(Optional.of(course()));
    doAnswer(invocation -> {
      Courseclass target = invocation.getArgument(0);
      target.setClassCode("EN-01"); target.setClassName("English 01");
      target.setStartDate(request.getStartDate()); target.setEndDate(request.getEndDate());
      target.setMaxStudents(20); target.setAppliedTuitionFee(request.getAppliedTuitionFee());
      target.setCourseId(course()); return null;
    }).when(mapper).updateEntity(any(), eq(request), any(), isNull());
    when(classes.save(any())).thenAnswer(i -> i.getArgument(0));

    service.create(request);

    ArgumentCaptor<Courseclass> captor = ArgumentCaptor.forClass(Courseclass.class);
    verify(classes).existsByClassCodeIgnoreCase("EN-01");
    verify(classes).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(ClassStatus.DRAFT);
    assertThat(captor.getValue().getCreatedAt()).isNotNull();
  }

  @Test
  void shouldRejectCreateWhenCodeDatesCapacityOrTuitionAreInvalid() {
    CourseClassRequest duplicate = request();
    when(classes.existsByClassCodeIgnoreCase("EN-01")).thenReturn(true);
    assertThatThrownBy(() -> service.create(duplicate)).isInstanceOf(DuplicateResourceException.class);
    reset(classes);
    CourseClassRequest invalidDates = request();
    invalidDates.setEndDate(invalidDates.getStartDate());
    assertThatThrownBy(() -> service.create(invalidDates)).isInstanceOf(IllegalArgumentException.class);
    CourseClassRequest invalidCapacity = request(); invalidCapacity.setMaxStudents(0);
    assertThatThrownBy(() -> service.create(invalidCapacity)).isInstanceOf(IllegalArgumentException.class);
    CourseClassRequest invalidTuition = request();
    invalidTuition.setAppliedTuitionFee(new BigDecimal("-1"));
    assertThatThrownBy(() -> service.create(invalidTuition)).isInstanceOf(IllegalArgumentException.class);
    verify(classes, never()).save(any());
  }

  @Test
  void shouldExcludeCurrentIdFromCodeCheckWhenUpdating() {
    Courseclass value = courseClass(ClassStatus.DRAFT);
    CourseClassRequest request = request();
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    when(courses.findByIdAndStatus(3, CatalogStatus.ACTIVE)).thenReturn(Optional.of(course()));
    when(classes.save(value)).thenReturn(value);

    service.update(1, request);

    verify(classes).existsByClassCodeIgnoreCaseAndIdNot("EN-01", 1);
    verify(classes, never()).existsByClassCodeIgnoreCase(anyString());
  }

  @Test
  void shouldAssignOnlyActiveTeacher() {
    Courseclass value = courseClass(ClassStatus.DRAFT);
    Teacher active = teacher(AccountStatus.ACTIVE);
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    when(teachers.findById(7)).thenReturn(Optional.of(active));
    when(classes.save(value)).thenReturn(value);
    service.assignTeacher(1, 7);
    assertThat(value.getTeacherId()).isSameAs(active);

    when(teachers.findById(8)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.assignTeacher(1, 8)).isInstanceOf(ResourceNotFoundException.class);
    when(teachers.findById(9)).thenReturn(Optional.of(teacher(AccountStatus.LOCKED)));
    assertThatThrownBy(() -> service.assignTeacher(1, 9)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectOpeningWhenTeacherOrScheduleIsMissing() {
    Courseclass value = courseClass(ClassStatus.DRAFT);
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    assertThatThrownBy(() -> service.changeStatus(1, ClassStatus.OPEN))
        .isInstanceOf(IllegalArgumentException.class);
    value.setTeacherId(teacher(AccountStatus.ACTIVE));
    when(schedules.findByCourseClassId_Id(1)).thenReturn(List.of());
    assertThatThrownBy(() -> service.changeStatus(1, ClassStatus.OPEN))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldPublishMailEventWhenDraftClassIsOpened() {
    Courseclass value = courseClass(ClassStatus.DRAFT);
    value.setTeacherId(teacher(AccountStatus.ACTIVE));
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    when(schedules.findByCourseClassId_Id(1)).thenReturn(List.of(schedule((short) 2, 8, 10)));
    when(classes.save(value)).thenReturn(value);

    service.changeStatus(1, ClassStatus.OPEN);

    verify(eventPublisher).publishEvent(any(ClassOpenedMailEvent.class));
  }

  @Test
  void shouldRejectOpeningWhenSchedulesOverlapInternally() {
    Courseclass value = courseClass(ClassStatus.DRAFT);
    value.setTeacherId(teacher(AccountStatus.ACTIVE));
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    when(schedules.findByCourseClassId_Id(1)).thenReturn(List.of(
        schedule((short) 2, 8, 10), schedule((short) 2, 9, 11)));

    assertThatThrownBy(() -> service.changeStatus(1, ClassStatus.OPEN))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectOpeningWhenRoomOrTeacherScheduleConflicts() {
    Courseclass value = courseClass(ClassStatus.DRAFT);
    value.setTeacherId(teacher(AccountStatus.ACTIVE));
    Classschedule schedule = schedule((short) 2, 8, 10);
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    when(schedules.findByCourseClassId_Id(1)).thenReturn(List.of(schedule));
    when(schedules.existsConflict(eq(1), isNull(), eq(7), any(), any(), eq((short) 2),
        any(), any())).thenReturn(true);

    assertThatThrownBy(() -> service.changeStatus(1, ClassStatus.OPEN))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldAllowFullOnlyWhenCapacityIsReached() {
    Courseclass value = courseClass(ClassStatus.OPEN);
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(eq(1), any())).thenReturn(19L);
    assertThatThrownBy(() -> service.changeStatus(1, ClassStatus.FULL))
        .isInstanceOf(IllegalArgumentException.class);
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(eq(1), any())).thenReturn(20L);
    when(classes.save(value)).thenReturn(value);
    service.changeStatus(1, ClassStatus.FULL);
    assertThat(value.getStatus()).isEqualTo(ClassStatus.FULL);
  }

  @Test
  void shouldLockClassBeforeChangingStatus() {
    Courseclass value = courseClass(ClassStatus.DRAFT);
    when(classes.lockById(1)).thenReturn(Optional.of(value));
    when(classes.save(value)).thenReturn(value);
    service.changeStatus(1, ClassStatus.CANCELLED);
    verify(classes).lockById(1);
    verify(classes).save(value);
  }

  @Test
  void shouldReturnOnlyClassesAssignedToCurrentTeacher() {
    Teacher teacher = teacher(AccountStatus.ACTIVE);
    Courseclass assigned = courseClass(ClassStatus.OPEN);
    when(teachers.findByUserId_EmailIgnoreCase("teacher@example.com"))
        .thenReturn(Optional.of(teacher));
    when(classes.findByTeacherId_IdOrderByStartDateDesc(7)).thenReturn(List.of(assigned));
    when(classes.findDistinctCoursesByTeacherId(7)).thenReturn(List.of(assigned.getCourseId()));
    when(mapper.toResponse(eq(assigned), anyLong())).thenReturn(mock(CourseClassResponse.class));
    when(courseMapper.toResponse(assigned.getCourseId())).thenReturn(mock(CourseResponse.class));

    assertThat(service.getTeacherClasses(() -> "teacher@example.com")).hasSize(1);
    assertThat(service.getTeacherCourses(() -> "teacher@example.com")).hasSize(1);
    verify(classes).findByTeacherId_IdOrderByStartDateDesc(7);
    verify(classes).findDistinctCoursesByTeacherId(7);
  }

  @Test
  void shouldRejectTeacherQueriesWhenPrincipalOrProfileIsInvalid() {
    assertThatThrownBy(() -> service.getTeacherClasses(null))
        .isInstanceOf(UnauthorizedException.class);
    assertThatThrownBy(() -> service.getTeacherCourses(() -> "  "))
        .isInstanceOf(UnauthorizedException.class);
    when(teachers.findByUserId_EmailIgnoreCase("missing@example.com")).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.getTeacherClasses(() -> "missing@example.com"))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  private CourseClassRequest request() {
    CourseClassRequest request = new CourseClassRequest();
    request.setClassCode(" en-01 "); request.setClassName(" English 01 ");
    request.setStartDate(date(30)); request.setEndDate(date(90)); request.setMaxStudents(20);
    request.setAppliedTuitionFee(new BigDecimal("1000000")); request.setCourseId(3);
    return request;
  }

  private Courseclass courseClass(ClassStatus status) {
    Courseclass value = new Courseclass(1); value.setClassCode("EN-01");
    value.setClassName("English 01"); value.setStartDate(date(30)); value.setEndDate(date(90));
    value.setMaxStudents(20); value.setAppliedTuitionFee(new BigDecimal("1000000"));
    value.setStatus(status); value.setCourseId(course()); value.setCreatedAt(new Date());
    return value;
  }

  private Course course() {
    Language language = new Language(4); language.setStatus(CatalogStatus.ACTIVE);
    Level level = new Level(2); level.setLanguageId(language); level.setLevelCode("A1");
    Course course = new Course(3); course.setCourseCode("EN-A1"); course.setCourseName("English A1");
    course.setLevelId(level); course.setStatus(CatalogStatus.ACTIVE);
    course.setPublicationStatus(PublicationStatus.PUBLISHED); return course;
  }

  private Teacher teacher(AccountStatus status) {
    User user = new User(); user.setFullName("Teacher"); user.setStatus(status);
    Teacher teacher = new Teacher(7, "GV007", 5); teacher.setUserId(user); return teacher;
  }

  private Classschedule schedule(short day, int startHour, int endHour) {
    Classschedule value = new Classschedule(); value.setDayOfWeek(day);
    value.setStartTime(time(startHour)); value.setEndTime(time(endHour)); return value;
  }

  private Date date(int days) {
    return Date.from(LocalDate.now().plusDays(days).atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private Date time(int hour) { return new Date(hour * 60L * 60L * 1000L); }
}
