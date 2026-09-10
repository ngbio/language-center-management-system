package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.*;
import com.ntt.language_center_management.dto.response.LessonResponse;
import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.exception.*;
import com.ntt.language_center_management.mapper.LessonMapper;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.impl.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LessonServiceImplTest {
  private LessonRepository lessons;
  private ClassScheduleRepository schedules;
  private CourseClassRepository classes;
  private AttendanceRepository attendance;
  private UserRepository users;
  private LessonMapper mapper;
  private LessonServiceImpl service;

  @BeforeEach
  void setUp() {
    lessons = mock(LessonRepository.class); schedules = mock(ClassScheduleRepository.class);
    classes = mock(CourseClassRepository.class); attendance = mock(AttendanceRepository.class);
    users = mock(UserRepository.class); mapper = mock(LessonMapper.class);
    service = new LessonServiceImpl(lessons, schedules, classes, attendance,
        mock(EnrollmentRepository.class), mock(StudentRepository.class), users, mapper,
        "Asia/Ho_Chi_Minh");
  }

  @Test
  void shouldGenerateLessonsOnConfiguredWeekdaysWithinClassDatesAndLimit() {
    Courseclass courseClass = courseClass(ClassStatus.OPEN, 2);
    Classschedule schedule = schedule(courseClass, DayOfWeek.MONDAY.getValue());
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass));
    when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user(10, "ADMIN")));
    when(schedules.findByCourseClassId_IdOrderByDayOfWeekAscStartTimeAsc(1))
        .thenReturn(List.of(schedule));
    when(lessons.findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(1))
        .thenReturn(List.of(), List.of());

    service.generate(1, () -> "admin@example.com");

    ArgumentCaptor<List<Lesson>> captor = ArgumentCaptor.forClass(List.class);
    verify(classes).lockById(1);
    verify(lessons).saveAll(captor.capture());
    assertThat(captor.getValue()).hasSize(2);
    assertThat(captor.getValue()).allSatisfy(lesson -> {
      LocalDate date = ((java.sql.Date) lesson.getLessonDate()).toLocalDate();
      assertThat(date.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);
      assertThat(date).isBetween(localDate(courseClass.getStartDate()), localDate(courseClass.getEndDate()));
      assertThat(lesson.getStatus()).isEqualTo(LessonStatus.SCHEDULED);
    });
  }

  @Test
  void shouldRejectTeacherGenerationBeforeClassStart() {
    Courseclass courseClass = courseClass(ClassStatus.OPEN, 2);
    User teacherUser = user(7, "TEACHER");
    Teacher teacher = new Teacher(3); teacher.setUserId(teacherUser); courseClass.setTeacherId(teacher);
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass));
    when(users.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(teacherUser));

    assertThatThrownBy(() -> service.generate(1, () -> "teacher@example.com"))
        .isInstanceOf(IllegalArgumentException.class);
    verify(lessons, never()).saveAll(any());
  }

  @Test
  void shouldSkipExistingLessonAndNeverExceedTotalSessions() {
    Courseclass courseClass = courseClass(ClassStatus.OPEN, 1);
    Classschedule schedule = schedule(courseClass, DayOfWeek.MONDAY.getValue());
    Lesson existing = lesson(schedule, next(DayOfWeek.MONDAY));
    when(classes.lockById(1)).thenReturn(Optional.of(courseClass));
    when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(user(10, "ADMIN")));
    when(schedules.findByCourseClassId_IdOrderByDayOfWeekAscStartTimeAsc(1)).thenReturn(List.of(schedule));
    when(lessons.findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(1))
        .thenReturn(List.of(existing));
    when(mapper.toResponse(existing)).thenReturn(mock(LessonResponse.class));

    assertThat(service.generate(1, () -> "admin@example.com")).hasSize(1);
    verify(lessons, never()).saveAll(any());
  }

  @Test
  void shouldAllowOnlyAssignedTeacherToUpdateAndTrimTopic() {
    Courseclass courseClass = courseClass(ClassStatus.OPEN, 2);
    User assigned = user(7, "TEACHER");
    Teacher teacher = new Teacher(3); teacher.setUserId(assigned); courseClass.setTeacherId(teacher);
    Lesson lesson = lesson(schedule(courseClass, 2), LocalDate.now().plusDays(5)); lesson.setId(9);
    when(lessons.lockById(9)).thenReturn(Optional.of(lesson));
    when(users.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(assigned));
    when(lessons.save(lesson)).thenReturn(lesson);
    when(mapper.toResponse(lesson)).thenReturn(mock(LessonResponse.class));

    service.update(9, new LessonUpdateRequest("  Topic  "), () -> "teacher@example.com");
    assertThat(lesson.getTopic()).isEqualTo("Topic");
    verify(lessons).lockById(9);

    when(users.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(user(8, "TEACHER")));
    assertThatThrownBy(() -> service.update(9, new LessonUpdateRequest("Other"),
        () -> "other@example.com")).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void shouldRescheduleOnlyInsideClassDatesWithoutConflictAndPreserveOriginalDate() {
    Courseclass courseClass = courseClass(ClassStatus.OPEN, 2);
    Classschedule schedule = schedule(courseClass, 2);
    LocalDate oldDate = LocalDate.now().plusDays(12);
    Lesson lesson = lesson(schedule, oldDate); lesson.setId(9);
    when(lessons.lockById(9)).thenReturn(Optional.of(lesson));
    when(lessons.save(lesson)).thenReturn(lesson);
    when(mapper.toResponse(lesson)).thenReturn(mock(LessonResponse.class));

    service.reschedule(9, new LessonRescheduleRequest(oldDate.plusDays(1), "  Holiday  "));

    assertThat(localDate(lesson.getOriginalLessonDate())).isEqualTo(oldDate);
    assertThat(localDate(lesson.getLessonDate())).isEqualTo(oldDate.plusDays(1));
    assertThat(lesson.getRescheduleReason()).isEqualTo("Holiday");
    verify(lessons).lockById(9);
  }

  @Test
  void shouldRejectRescheduleOutsideRangeOrOnResourceConflict() {
    Courseclass courseClass = courseClass(ClassStatus.OPEN, 2);
    Classschedule schedule = schedule(courseClass, 2);
    Lesson lesson = lesson(schedule, LocalDate.now().plusDays(12)); lesson.setId(9);
    when(lessons.lockById(9)).thenReturn(Optional.of(lesson));
    assertThatThrownBy(() -> service.reschedule(9,
        new LessonRescheduleRequest(LocalDate.now().plusDays(100), "reason")))
        .isInstanceOf(IllegalArgumentException.class);
    when(lessons.existsResourceConflictOnDate(eq(9), eq(1), any(), any(), any(), any(), any()))
        .thenReturn(true);
    assertThatThrownBy(() -> service.reschedule(9,
        new LessonRescheduleRequest(LocalDate.now().plusDays(13), "reason")))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  void shouldRejectRescheduleOrCancelWhenAttendanceExists() {
    Lesson lesson = lesson(schedule(courseClass(ClassStatus.OPEN, 2), 2), LocalDate.now().plusDays(12));
    lesson.setId(9);
    when(lessons.lockById(9)).thenReturn(Optional.of(lesson));
    when(attendance.existsByLessonId_Id(9)).thenReturn(true);
    assertThatThrownBy(() -> service.reschedule(9,
        new LessonRescheduleRequest(LocalDate.now().plusDays(13), "reason")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.cancel(9)).isInstanceOf(IllegalArgumentException.class);
    verify(lessons, never()).save(any());
  }

  @Test
  void shouldRejectChangesWhenClassOrLessonIsFinished() {
    Courseclass cancelled = courseClass(ClassStatus.CANCELLED, 2);
    Lesson lesson = lesson(schedule(cancelled, 2), LocalDate.now().plusDays(12)); lesson.setId(9);
    when(lessons.lockById(9)).thenReturn(Optional.of(lesson));
    assertThatThrownBy(() -> service.cancel(9)).isInstanceOf(IllegalArgumentException.class);
    lesson.setStatus(LessonStatus.COMPLETED);
    assertThatThrownBy(() -> service.reschedule(9,
        new LessonRescheduleRequest(LocalDate.now().plusDays(13), "reason")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldCompleteOnlyEndedScheduledOrInProgressLessonsUsingConfiguredZone() {
    LessonRepository repository = mock(LessonRepository.class);
    Courseclass active = courseClass(ClassStatus.OPEN, 2);
    Lesson scheduled = endedLesson(active, LessonStatus.SCHEDULED);
    Lesson inProgress = endedLesson(active, LessonStatus.IN_PROGRESS);
    Lesson future = lesson(schedule(active, 2), LocalDate.now());
    future.getClassScheduleId().setEndTime(java.sql.Time.valueOf("23:59:59"));
    when(repository.findByStatusInAndLessonDateLessThanEqual(any(), any()))
        .thenReturn(List.of(scheduled, inProgress, future));

    new LessonCompletionScheduler(repository, "Asia/Ho_Chi_Minh").completeEndedLessons();

    ArgumentCaptor<Collection<LessonStatus>> statuses = ArgumentCaptor.forClass(Collection.class);
    verify(repository).findByStatusInAndLessonDateLessThanEqual(statuses.capture(), any());
    assertThat(statuses.getValue()).containsExactlyInAnyOrder(
        LessonStatus.SCHEDULED, LessonStatus.IN_PROGRESS);
    verify(repository).saveAll(List.of(scheduled, inProgress));
    assertThat(scheduled.getStatus()).isEqualTo(LessonStatus.COMPLETED);
    assertThat(inProgress.getStatus()).isEqualTo(LessonStatus.COMPLETED);
    assertThat(future.getStatus()).isEqualTo(LessonStatus.SCHEDULED);
  }

  @Test
  void shouldNotSaveWhenSchedulerFindsNoEndedLessons() {
    LessonRepository repository = mock(LessonRepository.class);
    when(repository.findByStatusInAndLessonDateLessThanEqual(any(), any())).thenReturn(List.of());
    new LessonCompletionScheduler(repository, "Asia/Ho_Chi_Minh").completeEndedLessons();
    verify(repository, never()).saveAll(any());
  }

  @Test
  void shouldMapDateTimeRoomAndScheduleMeetingUrlUsingConfiguredZone() {
    Courseclass courseClass = courseClass(ClassStatus.OPEN, 2);
    courseClass.setClassCode("EN-01"); courseClass.setClassName("English");
    Classschedule schedule = schedule(courseClass, 2); schedule.setId(4);
    Room room = new Room(3); room.setRoomCode("P101"); room.setRoomName("Room 101");
    schedule.setRoomId(room); schedule.setDeliveryMode(DeliveryMode.ONLINE);
    schedule.setMeetingUrl("https://meet.example");
    Lesson lesson = lesson(schedule, LocalDate.of(2026, 9, 10)); lesson.setId(9);

    LessonResponse result = new LessonMapper("Asia/Ho_Chi_Minh").toResponse(lesson);

    assertThat(result.lessonDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    assertThat(result.startTime()).isEqualTo(LocalTime.of(8, 0));
    assertThat(result.roomCode()).isEqualTo("P101");
    assertThat(result.meetingUrl()).isEqualTo("https://meet.example");
  }

  private User user(int id, String roleCode) {
    User user = new User(); user.setId(id); user.setEmail(roleCode.toLowerCase() + "@example.com");
    user.setRoleId(new Role(1, roleCode, roleCode)); return user;
  }
  private Courseclass courseClass(ClassStatus status, int sessions) {
    Course course = new Course(2); course.setTotalSessions(sessions);
    Courseclass value = new Courseclass(1); value.setCourseId(course); value.setStatus(status);
    value.setStartDate(java.sql.Date.valueOf(LocalDate.now().plusDays(7)));
    value.setEndDate(java.sql.Date.valueOf(LocalDate.now().plusDays(40))); return value;
  }
  private Classschedule schedule(Courseclass owner, int day) {
    Classschedule value = new Classschedule(); value.setId(4); value.setCourseClassId(owner);
    value.setDayOfWeek((short) day); value.setStartTime(java.sql.Time.valueOf("08:00:00"));
    value.setEndTime(java.sql.Time.valueOf("10:00:00")); return value;
  }
  private Lesson lesson(Classschedule schedule, LocalDate date) {
    Lesson value = new Lesson(); value.setClassScheduleId(schedule);
    value.setLessonDate(java.sql.Date.valueOf(date)); value.setStatus(LessonStatus.SCHEDULED); return value;
  }
  private Lesson endedLesson(Courseclass owner, LessonStatus status) {
    Lesson value = lesson(schedule(owner, 2), LocalDate.now().minusDays(1)); value.setStatus(status); return value;
  }
  private LocalDate next(DayOfWeek day) {
    LocalDate date = LocalDate.now().plusDays(7);
    while (date.getDayOfWeek() != day) date = date.plusDays(1);
    return date;
  }
  private LocalDate localDate(Date date) { return ((java.sql.Date) date).toLocalDate(); }
}
