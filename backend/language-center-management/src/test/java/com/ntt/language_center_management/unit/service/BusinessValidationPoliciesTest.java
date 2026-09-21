package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.policy.*;
import com.ntt.language_center_management.repository.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class BusinessValidationPoliciesTest {
  private final ZoneId zone = ZoneId.of("Asia/Ho_Chi_Minh");

  @Test
  void springCanWireAllPoliciesAndInjectApplicationClockAndDefaultEditWindow() {
    try (var context = new org.springframework.context.annotation.AnnotationConfigApplicationContext()) {
      dependency(context, AttendanceRepository.class);
      dependency(context, LessonRepository.class);
      dependency(context, StudentRepository.class);
      dependency(context, EnrollmentRepository.class);
      dependency(context, TeacherRepository.class);
      dependency(context, RoomRepository.class);
      dependency(context, ClassScheduleRepository.class);
      dependency(context, CourseClassRepository.class);
      dependency(context, RefundRepository.class);
      dependency(context, UserRepository.class);
      dependency(context, QuizAttemptRepository.class);
      dependency(context, QuizQuestionRepository.class);
      dependency(context, QuizOptionRepository.class);
      dependency(context, org.springframework.security.crypto.password.PasswordEncoder.class);
      context.registerBean(Clock.class, () -> clock("2026-09-29T23:59:59"));
      context.register(com.ntt.language_center_management.security.CurrentUserResolver.class,
          LessonAccessPolicy.class, LessonChangePolicy.class,
          ScheduleChangePolicy.class, ScheduleLocationPolicy.class, ScheduleConflictChecker.class,
          CourseClassOpeningPolicy.class, CourseClassTransitionPolicy.class, CourseClassChangePolicy.class,
          AttendanceAccessPolicy.class, AttendanceUpdatePolicy.class,
          PaymentEligibilityPolicy.class, RefundEligibilityPolicy.class,
          AccountUniquenessValidator.class, PasswordChangePolicy.class,
          LearningAccessPolicy.class, QuizEditingPolicy.class, QuizAttemptPolicy.class, QuizSubmissionValidator.class);
      context.refresh();
      assertThatCode(() -> context.getBean(AttendanceUpdatePolicy.class).ensureCanUpdateAttendance(lesson()))
          .doesNotThrowAnyException();
      assertThat(context.getBean(LessonChangePolicy.class)).isNotNull();
    }
  }

  private <T> void dependency(org.springframework.context.annotation.AnnotationConfigApplicationContext context,
      Class<T> type) {
    context.registerBean(type, () -> mock(type));
  }

  private Clock clock(String time) {
    return Clock.fixed(LocalDateTime.parse(time).atZone(zone).toInstant(), zone);
  }

  private Lesson lesson() {
    var courseClass = new Courseclass(1);
    courseClass.setStatus(ClassStatus.OPEN);
    var schedule = new Classschedule();
    schedule.setCourseClassId(courseClass);
    schedule.setStartTime(java.sql.Time.valueOf("08:00:00"));
    var lesson = new Lesson(1);
    lesson.setStatus(LessonStatus.SCHEDULED);
    lesson.setLessonDate(java.sql.Date.valueOf("2026-09-22"));
    lesson.setClassScheduleId(schedule);
    return lesson;
  }

  @Test
  void attendanceStartsAtExactLessonStartInApplicationZone() {
    var lesson = lesson();
    assertThatThrownBy(() -> new AttendanceUpdatePolicy(7, clock("2026-09-22T07:59:59"))
        .ensureCanUpdateAttendance(lesson)).hasMessageContaining("sau thời gian bắt đầu");
    assertThatCode(() -> new AttendanceUpdatePolicy(7, clock("2026-09-22T08:00:00"))
        .ensureCanUpdateAttendance(lesson)).doesNotThrowAnyException();
  }

  @Test
  void attendanceEditWindowIncludesEntireLastCalendarDay() {
    var lesson = lesson();
    assertThatCode(() -> new AttendanceUpdatePolicy(7, clock("2026-09-29T23:59:59"))
        .ensureCanUpdateAttendance(lesson)).doesNotThrowAnyException();
    assertThatThrownBy(() -> new AttendanceUpdatePolicy(7, clock("2026-09-30T00:00:00"))
        .ensureCanUpdateAttendance(lesson)).hasMessageContaining("quá thời hạn");
  }

  @Test
  void cancelledLessonFailsBeforeTimeValidation() {
    var lesson = lesson();
    lesson.setStatus(LessonStatus.CANCELLED);
    assertThatThrownBy(() -> new AttendanceUpdatePolicy(7, clock("2026-09-22T07:00:00"))
        .ensureCanUpdateAttendance(lesson)).hasMessageContaining("buổi học đã hủy");
  }

  @Test
  void classOpeningAllowsTodayButRejectsYesterdayBeforeScheduleQueries() {
    var schedules = mock(ClassScheduleRepository.class);
    var policy = new CourseClassOpeningPolicy(schedules, clock("2026-09-22T00:00:00"));
    var value = lesson().getClassScheduleId().getCourseClassId();
    var course = new Course(1);
    course.setStatus(CatalogStatus.ACTIVE);
    value.setCourseId(course);
    var user = new User(1);
    user.setStatus(AccountStatus.ACTIVE);
    var teacher = new Teacher(1);
    teacher.setUserId(user);
    value.setTeacherId(teacher);
    value.setStartDate(java.sql.Date.valueOf("2026-09-21"));
    assertThatThrownBy(() -> policy.validateCanOpen(value)).hasMessageContaining("qua ngày bắt đầu");
    verifyNoInteractions(schedules);

    value.setStartDate(java.sql.Date.valueOf("2026-09-22"));
    value.setEndDate(java.sql.Date.valueOf("2026-10-22"));
    var schedule = lesson().getClassScheduleId();
    schedule.setDayOfWeek((short) 2);
    schedule.setEndTime(java.sql.Time.valueOf("10:00:00"));
    when(schedules.findByCourseClassId_Id(1)).thenReturn(List.of(schedule));
    assertThatCode(() -> policy.validateCanOpen(value)).doesNotThrowAnyException();
  }

  @Test
  void lessonCannotBeRescheduledAtExactStartTime() {
    var attendance = mock(AttendanceRepository.class);
    var lessons = mock(LessonRepository.class);
    var value = lesson();
    var courseClass = value.getClassScheduleId().getCourseClassId();
    courseClass.setStartDate(java.sql.Date.valueOf("2026-09-01"));
    courseClass.setEndDate(java.sql.Date.valueOf("2026-10-01"));
    assertThatThrownBy(() -> new LessonChangePolicy(attendance, lessons).validateReschedule(value,
        new com.ntt.language_center_management.dto.request.LessonRescheduleRequest(LocalDate.of(2026, 9, 23), "Reason"),
        LocalDateTime.of(2026, 9, 22, 8, 0), zone)).hasMessageContaining("chưa bắt đầu");
    verifyNoInteractions(lessons);
  }
}
