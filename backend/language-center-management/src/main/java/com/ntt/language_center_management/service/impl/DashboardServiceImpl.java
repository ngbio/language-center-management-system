package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.response.DashboardSummaryResponse;
import com.ntt.language_center_management.dto.response.EnrollmentReportResponse;
import com.ntt.language_center_management.dto.response.PopularCourseReportResponse;
import com.ntt.language_center_management.dto.response.RevenueReportResponse;
import com.ntt.language_center_management.dto.response.TeacherLoadReportResponse;
import com.ntt.language_center_management.dto.response.UpcomingClassReportResponse;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.CourseRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.repository.RefundRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.service.DashboardService;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.ntt.language_center_management.policy.EnrollmentPolicy.CAPACITY_RESERVED_STATUSES;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {
  private static final int MAX_REPORT_DAYS = 366;
  private static final Set<ClassStatus> ACTIVE_CLASS_STATUSES =
      Set.of(ClassStatus.OPEN, ClassStatus.IN_PROGRESS);
  private static final Set<ClassStatus> UPCOMING_CLASS_STATUSES =
      Set.of(ClassStatus.DRAFT, ClassStatus.OPEN);

  private final StudentRepository students;
  private final TeacherRepository teachers;
  private final CourseRepository courses;
  private final CourseClassRepository classes;
  private final EnrollmentRepository enrollments;
  private final PaymentRepository payments;
  private final RefundRepository refunds;
  private final LessonRepository lessons;
  private final ZoneId zoneId;

  public DashboardServiceImpl(StudentRepository students, TeacherRepository teachers,
      CourseRepository courses, CourseClassRepository classes, EnrollmentRepository enrollments,
      PaymentRepository payments, RefundRepository refunds, LessonRepository lessons,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String timeZone) {
    this.students = students;
    this.teachers = teachers;
    this.courses = courses;
    this.classes = classes;
    this.enrollments = enrollments;
    this.payments = payments;
    this.refunds = refunds;
    this.lessons = lessons;
    this.zoneId = ZoneId.of(timeZone);
  }

  @Override
  public DashboardSummaryResponse getSummary() {
    LocalDate today = LocalDate.now(zoneId);
    BigDecimal gross = zero(payments.sumPaidAmount());
    BigDecimal refunded = zero(refunds.sumCompletedAmount());
    return new DashboardSummaryResponse(students.count(), teachers.count(), courses.count(),
        classes.countByStatusIn(ACTIVE_CLASS_STATUSES),
        classes.countByStartDateBetweenAndStatusIn(dateOnly(today), dateOnly(today.plusDays(30)),
            UPCOMING_CLASS_STATUSES),
        enrollments.countByEnrollmentStatusAndPaymentStatus(
            EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PENDING),
        enrollments.countByEnrollmentStatusAndPaymentStatus(
            EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PAID),
        gross, refunded, gross.subtract(refunded));
  }

  @Override
  public List<RevenueReportResponse> getRevenue(LocalDate from, LocalDate to) {
    DateRange range = validateRange(from, to);
    Map<YearMonth, BigDecimal> paid = monthlyMoney(
        payments.aggregatePaidByMonth(range.from(), range.toExclusive()));
    Map<YearMonth, BigDecimal> returned = monthlyMoney(
        refunds.aggregateCompletedByMonth(range.from(), range.toExclusive()));
    List<RevenueReportResponse> result = new ArrayList<>();
    for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to));
        month = month.plusMonths(1)) {
      BigDecimal gross = paid.getOrDefault(month, BigDecimal.ZERO);
      BigDecimal refunded = returned.getOrDefault(month, BigDecimal.ZERO);
      result.add(new RevenueReportResponse(month, gross, refunded, gross.subtract(refunded)));
    }
    return result;
  }

  @Override
  public List<EnrollmentReportResponse> getEnrollments(LocalDate from, LocalDate to) {
    DateRange range = validateRange(from, to);
    Map<YearMonth, long[]> valuesByMonth = new LinkedHashMap<>();
    for (Object[] row : enrollments.aggregateByMonth(range.from(), range.toExclusive())) {
      valuesByMonth.put(YearMonth.of(number(row[0]).intValue(), number(row[1]).intValue()),
          new long[] {number(row[2]).longValue(), number(row[3]).longValue(),
              number(row[4]).longValue(), number(row[5]).longValue()});
    }
    List<EnrollmentReportResponse> result = new ArrayList<>();
    for (YearMonth month = YearMonth.from(from); !month.isAfter(YearMonth.from(to));
        month = month.plusMonths(1)) {
      long[] values = valuesByMonth.getOrDefault(month, new long[4]);
      result.add(new EnrollmentReportResponse(month, values[0], values[1], values[2], values[3]));
    }
    return result;
  }

  @Override
  public List<PopularCourseReportResponse> getPopularCourses(LocalDate from, LocalDate to,
      int limit) {
    DateRange range = validateRange(from, to);
    if (limit < 1 || limit > 20) {
      throw new IllegalArgumentException("Giới hạn khóa học phổ biến phải từ 1 đến 20");
    }
    return enrollments
        .findPopularCourses(range.from(), range.toExclusive(), PageRequest.of(0, limit))
        .stream()
        .map(row -> new PopularCourseReportResponse(number(row[0]).intValue(),
            String.valueOf(row[1]), String.valueOf(row[2]), number(row[3]).longValue(),
            number(row[4]).longValue(), decimal(row[5])))
        .toList();
  }

  @Override
  public List<TeacherLoadReportResponse> getTeacherLoad(LocalDate from, LocalDate to) {
    validateRange(from, to);
    return lessons.aggregateTeacherLoad(dateOnly(from), dateOnly(to)).stream()
        .map(row -> new TeacherLoadReportResponse(number(row[0]).intValue(),
            String.valueOf(row[1]), String.valueOf(row[2]), number(row[3]).longValue(),
            number(row[4]).longValue(), number(row[5]).longValue()))
        .toList();
  }

  @Override
  public List<UpcomingClassReportResponse> getUpcomingClasses(LocalDate from, LocalDate to) {
    validateRange(from, to);
    List<Courseclass> upcomingClasses =
        classes.findByStartDateBetweenAndStatusInOrderByStartDateAsc(
            dateOnly(from), dateOnly(to), UPCOMING_CLASS_STATUSES);
    List<Integer> classIds = upcomingClasses.stream().map(Courseclass::getId).toList();
    Map<Integer, Long> reservedByClass = classIds.isEmpty()
        ? Map.of()
        : enrollments
            .countByCourseClassIdsAndEnrollmentStatusIn(
                classIds, CAPACITY_RESERVED_STATUSES)
            .stream()
            .collect(Collectors.toMap(
                value -> value.getCourseClassId(),
                value -> value.getEnrollmentCount()));
    return upcomingClasses.stream()
        .map(value -> toUpcomingClass(value, reservedByClass.getOrDefault(value.getId(), 0L)))
        .toList();
  }

  private UpcomingClassReportResponse toUpcomingClass(Courseclass value, long reserved) {
    String teacher = value.getTeacherId() == null ? null : value.getTeacherId().getUserId().getFullName();
    return new UpcomingClassReportResponse(value.getId(), value.getClassCode(), value.getClassName(),
        value.getCourseId().getId(), value.getCourseId().getCourseName(), teacher,
        toLocalDate(value.getStartDate()), toLocalDate(value.getEndDate()), value.getMaxStudents(),
        reserved, Math.max(0, value.getMaxStudents() - reserved), value.getAppliedTuitionFee(),
        value.getStatus().name());
  }

  private DateRange validateRange(LocalDate from, LocalDate to) {
    if (from == null || to == null) throw new IllegalArgumentException("Khoảng thời gian báo cáo là bắt buộc");
    if (from.isAfter(to)) throw new IllegalArgumentException("Ngày bắt đầu không được sau ngày kết thúc");
    if (ChronoUnit.DAYS.between(from, to) > MAX_REPORT_DAYS) {
      throw new IllegalArgumentException("Khoảng thời gian báo cáo không được vượt quá 366 ngày");
    }
    return new DateRange(startOfDay(from), startOfDay(to.plusDays(1)));
  }

  private Map<YearMonth, BigDecimal> monthlyMoney(List<Object[]> rows) {
    Map<YearMonth, BigDecimal> result = new LinkedHashMap<>();
    for (Object[] row : rows) {
      result.put(YearMonth.of(number(row[0]).intValue(), number(row[1]).intValue()), decimal(row[2]));
    }
    return result;
  }

  private Date startOfDay(LocalDate value) {
    return Date.from(value.atStartOfDay(zoneId).toInstant());
  }

  private Date dateOnly(LocalDate value) {
    return java.sql.Date.valueOf(value);
  }

  private LocalDate toLocalDate(Date value) {
    return ApplicationDateTimeUtils.toLocalDate(value, zoneId);
  }

  private Number number(Object value) {
    return value instanceof Number number ? number : 0;
  }

  private BigDecimal decimal(Object value) {
    return value instanceof BigDecimal decimal ? decimal : new BigDecimal(String.valueOf(value));
  }

  private BigDecimal zero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private record DateRange(Date from, Date toExclusive) {}
}
