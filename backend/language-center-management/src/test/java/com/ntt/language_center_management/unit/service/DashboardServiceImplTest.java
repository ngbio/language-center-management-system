package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.CourseRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.repository.RefundRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.repository.projection.CourseClassEnrollmentCount;
import com.ntt.language_center_management.service.impl.DashboardServiceImpl;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class DashboardServiceImplTest {
  private StudentRepository students;
  private TeacherRepository teachers;
  private CourseRepository courses;
  private CourseClassRepository classes;
  private EnrollmentRepository enrollments;
  private PaymentRepository payments;
  private RefundRepository refunds;
  private LessonRepository lessons;
  private DashboardServiceImpl service;

  @BeforeEach
  void setUp() {
    students = mock(StudentRepository.class);
    teachers = mock(TeacherRepository.class);
    courses = mock(CourseRepository.class);
    classes = mock(CourseClassRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    payments = mock(PaymentRepository.class);
    refunds = mock(RefundRepository.class);
    lessons = mock(LessonRepository.class);
    service = new DashboardServiceImpl(students, teachers, courses, classes, enrollments,
        payments, refunds, lessons, "Asia/Ho_Chi_Minh");
  }

  @Test
  void shouldReplaceNullAggregatesWithZeroInSummary() {
    when(students.count()).thenReturn(10L);
    when(teachers.count()).thenReturn(3L);
    when(courses.count()).thenReturn(5L);
    when(classes.countByStatusIn(any())).thenReturn(2L);
    when(classes.countByStartDateBetweenAndStatusIn(any(), any(), any())).thenReturn(1L);
    when(enrollments.countByEnrollmentStatusAndPaymentStatus(any(), any())).thenReturn(4L, 6L);
    when(payments.sumPaidAmount()).thenReturn(null);
    when(refunds.sumCompletedAmount()).thenReturn(null);

    var summary = service.getSummary();

    assertThat(summary.totalStudents()).isEqualTo(10);
    assertThat(summary.pendingEnrollments()).isEqualTo(4);
    assertThat(summary.grossRevenue()).isZero();
    assertThat(summary.refundedAmount()).isZero();
    assertThat(summary.netRevenue()).isZero();
  }

  @Test
  void shouldValidateMissingReversedAndOversizedDateRanges() {
    LocalDate today = LocalDate.now();
    assertThatThrownBy(() -> service.getRevenue(null, today)).hasMessageContaining("bắt buộc");
    assertThatThrownBy(() -> service.getRevenue(today, today.minusDays(1)))
        .hasMessageContaining("không được sau");
    assertThatThrownBy(() -> service.getRevenue(today, today.plusDays(367)))
        .hasMessageContaining("366 ngày");
  }

  @Test
  void shouldFillMissingRevenueMonthsAndSubtractRefunds() {
    LocalDate from = LocalDate.of(2026, 1, 1);
    LocalDate to = LocalDate.of(2026, 3, 31);
    when(payments.aggregatePaidByMonth(any(), any())).thenReturn(List.of(
        new Object[] {2026, 1, new BigDecimal("1000")},
        new Object[] {2026, 3, new BigDecimal("500")}));
    when(refunds.aggregateCompletedByMonth(any(), any())).thenReturn(
        java.util.Collections.singletonList(new Object[] {2026, 1, new BigDecimal("200")}));

    var result = service.getRevenue(from, to);

    assertThat(result).hasSize(3);
    assertThat(result.get(0).month()).isEqualTo(YearMonth.of(2026, 1));
    assertThat(result.get(0).netRevenue()).isEqualByComparingTo("800");
    assertThat(result.get(1).grossRevenue()).isZero();
    assertThat(result.get(2).netRevenue()).isEqualByComparingTo("500");
  }

  @Test
  void shouldMapEnrollmentNativeNumbersAndFillMissingMonth() {
    when(enrollments.aggregateByMonth(any(), any())).thenReturn(List.<Object[]>of(
        new Object[] {(short) 2026, (byte) 2, 9L, 7, new BigDecimal("5"), 2L}));

    var result = service.getEnrollments(
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 28));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).total()).isZero();
    assertThat(result.get(1).total()).isEqualTo(9);
    assertThat(result.get(1).confirmed()).isEqualTo(7);
    assertThat(result.get(1).paid()).isEqualTo(5);
    assertThat(result.get(1).cancelled()).isEqualTo(2);
  }

  @Test
  void shouldValidatePopularCourseLimitAndMapNativeRow() {
    LocalDate date = LocalDate.of(2026, 1, 1);
    assertThatThrownBy(() -> service.getPopularCourses(date, date, 0))
        .hasMessageContaining("1 đến 20");
    assertThatThrownBy(() -> service.getPopularCourses(date, date, 21))
        .hasMessageContaining("1 đến 20");
    when(enrollments.findPopularCourses(any(), any(), any(Pageable.class))).thenReturn(List.<Object[]>of(
        new Object[] {3L, "EN-A1", "English A1", 12L, 10L, "32000000.00"}));

    var result = service.getPopularCourses(date, date, 5);

    assertThat(result).singleElement().satisfies(value -> {
      assertThat(value.courseId()).isEqualTo(3);
      assertThat(value.paidEnrollments()).isEqualTo(10);
      assertThat(value.revenue()).isEqualByComparingTo("32000000");
    });
  }

  @Test
  void shouldMapTeacherLoadAndUpcomingClassWithNullableTeacher() {
    LocalDate from = LocalDate.of(2026, 9, 1);
    LocalDate to = LocalDate.of(2026, 9, 30);
    when(lessons.aggregateTeacherLoad(any(), any())).thenReturn(List.<Object[]>of(
        new Object[] {4, "GV004", "Teacher A", 2L, 20L, 18L}));
    Courseclass noTeacher = upcomingClass(10, null);
    Courseclass assigned = upcomingClass(11, "Teacher B");
    when(classes.findByStartDateBetweenAndStatusInOrderByStartDateAsc(any(), any(), any()))
        .thenReturn(List.of(noTeacher, assigned));
    CourseClassEnrollmentCount firstCount = mock(CourseClassEnrollmentCount.class);
    CourseClassEnrollmentCount secondCount = mock(CourseClassEnrollmentCount.class);
    when(firstCount.getCourseClassId()).thenReturn(10);
    when(firstCount.getEnrollmentCount()).thenReturn(12L);
    when(secondCount.getCourseClassId()).thenReturn(11);
    when(secondCount.getEnrollmentCount()).thenReturn(40L);
    when(enrollments.countByCourseClassIdsAndEnrollmentStatusIn(any(), any()))
        .thenReturn(List.of(firstCount, secondCount));

    assertThat(service.getTeacherLoad(from, to)).singleElement().satisfies(value -> {
      assertThat(value.teacherCode()).isEqualTo("GV004");
      assertThat(value.completedLessons()).isEqualTo(18);
    });
    var upcoming = service.getUpcomingClasses(from, to);
    assertThat(upcoming.get(0).teacherName()).isNull();
    assertThat(upcoming.get(0).availableSeats()).isEqualTo(18);
    assertThat(upcoming.get(1).teacherName()).isEqualTo("Teacher B");
    assertThat(upcoming.get(1).availableSeats()).isZero();
  }

  private Courseclass upcomingClass(int id, String teacherName) {
    Course course = new Course(3);
    course.setCourseName("English A1");
    Courseclass value = new Courseclass(id);
    value.setClassCode("CLASS-" + id);
    value.setClassName("Class " + id);
    value.setCourseId(course);
    value.setStartDate(Date.valueOf("2026-09-10"));
    value.setEndDate(Date.valueOf("2026-11-10"));
    value.setMaxStudents(30);
    value.setAppliedTuitionFee(new BigDecimal("3200000"));
    value.setStatus(ClassStatus.OPEN);
    if (teacherName != null) {
      User user = new User(id + 100);
      user.setFullName(teacherName);
      Teacher teacher = new Teacher(id + 200);
      teacher.setUserId(user);
      value.setTeacherId(teacher);
    }
    return value;
  }
}
