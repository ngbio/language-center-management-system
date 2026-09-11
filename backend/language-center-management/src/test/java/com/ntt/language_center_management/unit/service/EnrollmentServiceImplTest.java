package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.CancelEnrollmentRequest;
import com.ntt.language_center_management.dto.request.CreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.StaffCreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.TransferEnrollmentRequest;
import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.mapper.CourseClassMapper;
import com.ntt.language_center_management.mapper.CourseMapper;
import com.ntt.language_center_management.mapper.EnrollmentMapper;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.impl.EnrollmentServiceImpl;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class EnrollmentServiceImplTest {
  private EnrollmentRepository enrollments;
  private CourseClassRepository classes;
  private StudentRepository students;
  private UserRepository users;
  private EnrollmentMapper mapper;
  private CourseMapper courseMapper;
  private CourseClassMapper classMapper;
  private ClassScheduleMapper scheduleMapper;
  private ClassScheduleRepository schedules;
  private EnrollmentServiceImpl service;
  private CurrentUserResolver currentUserResolver;
  private EnrollmentResponse response;

  @BeforeEach
  void setUp() {
    enrollments = mock(EnrollmentRepository.class);
    classes = mock(CourseClassRepository.class);
    students = mock(StudentRepository.class);
    users = mock(UserRepository.class);
    mapper = mock(EnrollmentMapper.class);
    courseMapper = mock(CourseMapper.class);
    classMapper = mock(CourseClassMapper.class);
    scheduleMapper = mock(ClassScheduleMapper.class);
    schedules = mock(ClassScheduleRepository.class);
    currentUserResolver = new CurrentUserResolver(users, students, mock(TeacherRepository.class));
    service = new EnrollmentServiceImpl(enrollments, classes, students, users, mapper,
        courseMapper, classMapper, scheduleMapper, schedules, currentUserResolver);
    response = mock(EnrollmentResponse.class);
    when(mapper.toResponse(any(Enrollment.class))).thenReturn(response);
    when(enrollments.saveAndFlush(any(Enrollment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(enrollments.save(any(Enrollment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void shouldEnrollCurrentStudentAndSetTwoDayDeadline() {
    Student student = activeStudent(7, 70, "student@example.com");
    Courseclass courseClass = openClass(11, 20, "3200000", 30);
    mockCurrentStudent(student);
    when(classes.lockById(11)).thenReturn(Optional.of(courseClass));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(anyInt(), anySet()))
        .thenReturn(0L);

    EnrollmentResponse actual = service.enrollMe(new CreateEnrollmentRequest(11, null), principal());

    ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
    verify(enrollments).saveAndFlush(captor.capture());
    Enrollment saved = captor.getValue();
    assertThat(actual).isSameAs(response);
    assertThat(saved.getStudentId()).isSameAs(student);
    assertThat(saved.getCourseClassId()).isSameAs(courseClass);
    assertThat(saved.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(saved.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PENDING);
    assertThat(Duration.between(saved.getEnrollmentDate().toInstant(),
        saved.getPaymentDeadline().toInstant())).isEqualTo(Duration.ofDays(2));
    assertThat(saved.getConfirmedAt()).isEqualTo(saved.getEnrollmentDate());
  }

  @Test
  void shouldMarkFreeEnrollmentPaidAndLastAvailableClassFull() {
    Student student = activeStudent(7, 70, "student@example.com");
    Courseclass courseClass = openClass(11, 1, "0", 30);
    mockCurrentStudent(student);
    when(classes.lockById(11)).thenReturn(Optional.of(courseClass));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(anyInt(), anySet()))
        .thenReturn(0L, 1L);

    service.enrollMe(new CreateEnrollmentRequest(11, null), principal());

    ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
    verify(enrollments).saveAndFlush(captor.capture());
    assertThat(captor.getValue().getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PAID);
    assertThat(courseClass.getStatus()).isEqualTo(ClassStatus.FULL);
  }

  @Test
  void shouldTrimStudentEmailWhenStaffEnrolls() {
    Student student = activeStudent(7, 70, "student@example.com");
    Courseclass courseClass = openClass(11, 20, "100000", 30);
    when(students.findByUserId_EmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
    when(classes.lockById(11)).thenReturn(Optional.of(courseClass));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(anyInt(), anySet())).thenReturn(0L);

    service.enrollByStaff(new StaffCreateEnrollmentRequest(11, "  student@example.com  "));

    verify(students).findByUserId_EmailIgnoreCase("student@example.com");
    verify(enrollments).saveAndFlush(any(Enrollment.class));
  }

  @Test
  void shouldRejectInactiveStudentBeforeLockingClass() {
    Student student = activeStudent(7, 70, "student@example.com");
    student.getUserId().setStatus(AccountStatus.INACTIVE);
    mockCurrentUser(student.getUserId());
    when(students.findByUserId_EmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));

    assertThatThrownBy(() -> service.enrollMe(new CreateEnrollmentRequest(11, null), principal()))
        .isInstanceOf(ForbiddenException.class);

    verify(classes, never()).lockById(anyInt());
    verify(enrollments, never()).saveAndFlush(any());
  }

  @Test
  void shouldRejectClassThatIsNotOpenOrHasNoCapacity() {
    Student student = activeStudent(7, 70, "student@example.com");
    mockCurrentStudent(student);
    Courseclass draft = openClass(11, 20, "100000", 30);
    draft.setStatus(ClassStatus.DRAFT);
    when(classes.lockById(11)).thenReturn(Optional.of(draft));

    assertThatThrownBy(() -> service.enrollMe(new CreateEnrollmentRequest(11, null), principal()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không mở");

    Courseclass fullByCount = openClass(12, 2, "100000", 30);
    when(classes.lockById(12)).thenReturn(Optional.of(fullByCount));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(anyInt(), anySet())).thenReturn(2L);
    assertThatThrownBy(() -> service.enrollMe(new CreateEnrollmentRequest(12, null), principal()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("đủ số lượng");
  }

  @Test
  void shouldRejectDuplicateAndScheduleConflictBeforeSaving() {
    Student student = activeStudent(7, 70, "student@example.com");
    mockCurrentStudent(student);
    when(classes.lockById(11)).thenReturn(Optional.of(openClass(11, 20, "100000", 30)));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(anyInt(), anySet())).thenReturn(0L);
    when(enrollments.existsByStudentId_IdAndCourseClassId_Id(7, 11)).thenReturn(true);

    assertThatThrownBy(() -> service.enrollMe(new CreateEnrollmentRequest(11, null), principal()))
        .isInstanceOf(DuplicateResourceException.class);

    when(enrollments.existsByStudentId_IdAndCourseClassId_Id(7, 11)).thenReturn(false);
    when(enrollments.existsScheduleConflict(anyInt(), anyInt(), anySet())).thenReturn(true);
    assertThatThrownBy(() -> service.enrollMe(new CreateEnrollmentRequest(11, null), principal()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("trùng thời gian");
    verify(enrollments, never()).saveAndFlush(any());
  }

  @Test
  void shouldCancelOwnPendingEnrollmentAndReopenFullClass() {
    Student student = activeStudent(7, 70, "student@example.com");
    Courseclass courseClass = openClass(11, 20, "100000", 30);
    courseClass.setStatus(ClassStatus.FULL);
    Enrollment enrollment = enrollment(15, student, courseClass,
        EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PENDING);
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
    mockCurrentStudent(student);
    when(classes.lockById(11)).thenReturn(Optional.of(courseClass));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(anyInt(), anySet())).thenReturn(19L);

    service.requestCancel(15, new CancelEnrollmentRequest("  Đổi kế hoạch  "), principal());

    assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.CANCELLED);
    assertThat(enrollment.getCancellationReason()).isEqualTo("Đổi kế hoạch");
    assertThat(enrollment.getCancelledAt()).isNotNull();
    assertThat(courseClass.getStatus()).isEqualTo(ClassStatus.OPEN);
  }

  @Test
  void shouldRejectCancellationByAnotherStudentOrAfterPayment() {
    Student owner = activeStudent(7, 70, "owner@example.com");
    Student current = activeStudent(8, 80, "current@example.com");
    Enrollment enrollment = enrollment(15, owner, openClass(11, 20, "100000", 30),
        EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PENDING);
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
    mockCurrentStudent(current);

    assertThatThrownBy(() -> service.requestCancel(15,
        new CancelEnrollmentRequest("Không học"), () -> "current@example.com"))
        .isInstanceOf(ForbiddenException.class);

    mockCurrentStudent(owner);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PAID);
    assertThatThrownBy(() -> service.requestCancel(15,
        new CancelEnrollmentRequest("Không học"), () -> "owner@example.com"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hoàn tiền");
  }

  @Test
  void shouldRejectInvalidStaffStatusTransitions() {
    Enrollment cancelled = enrollment(15, activeStudent(7, 70, "student@example.com"),
        openClass(11, 20, "100000", 30), EnrollmentStatus.CANCELLED,
        EnrollmentPaymentStatus.CANCELLED);
    when(enrollments.lockById(15)).thenReturn(Optional.of(cancelled));

    assertThatThrownBy(() -> service.changeStatus(15, EnrollmentStatus.CONFIRMED))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("đã hủy");

    cancelled.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    assertThatThrownBy(() -> service.changeStatus(15, EnrollmentStatus.PENDING))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("chờ xử lý");
  }

  @Test
  void shouldTransferWithinSameCourseAndLockClassesInAscendingOrder() {
    Student student = activeStudent(7, 70, "student@example.com");
    Course course = new Course(3);
    Courseclass source = openClass(20, 1, "100000", 30);
    source.setStatus(ClassStatus.FULL);
    source.setCourseId(course);
    Courseclass target = openClass(10, 2, "120000", 30);
    target.setCourseId(course);
    Enrollment enrollment = enrollment(15, student, source,
        EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PENDING);
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
    when(classes.lockById(10)).thenReturn(Optional.of(target));
    when(classes.lockById(20)).thenReturn(Optional.of(source));
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(eq(10), anySet()))
        .thenReturn(0L, 2L);
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(eq(20), anySet())).thenReturn(0L);

    service.transfer(15, new TransferEnrollmentRequest(10));

    InOrder order = inOrder(classes);
    order.verify(classes).lockById(10);
    order.verify(classes).lockById(20);
    assertThat(enrollment.getCourseClassId()).isSameAs(target);
    assertThat(enrollment.getAmountDue()).isEqualByComparingTo("120000");
    assertThat(source.getStatus()).isEqualTo(ClassStatus.OPEN);
    assertThat(target.getStatus()).isEqualTo(ClassStatus.FULL);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldSearchStaffEnrollmentsWithNormalizedFiltersSortAndPagination() {
    Enrollment value = new Enrollment(1);
    when(enrollments.findAll(any(Specification.class), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(value)));

    var result = service.searchStaffEnrollments(" student ", 3, 4, " confirmed ",
        " paid ", 1, 10, "amountDue", "asc");

    assertThat(result.content()).containsExactly(response);
    ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
    verify(enrollments).findAll(any(Specification.class), pageable.capture());
    assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
    assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
    assertThat(pageable.getValue().getSort().getOrderFor("amountDue").isAscending()).isTrue();

    assertThatThrownBy(() -> service.searchStaffEnrollments(null, null, null, "invalid",
        null, 0, 20, null, null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.searchStaffEnrollments(null, null, null, null,
        "invalid", 0, 20, null, null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.searchStaffEnrollments(null, null, null, null,
        null, -1, 20, null, null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.searchStaffEnrollments(null, null, null, null,
        null, 0, 101, null, null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldReturnStudentEnrollmentCourseClassAndScheduleViews() {
    Student student = activeStudent(7, 70, "student@example.com");
    mockCurrentStudent(student);
    Enrollment enrollment = new Enrollment(1);
    Course course = new Course(3);
    Courseclass courseClass = new Courseclass(4);
    Classschedule schedule = new Classschedule(5);
    EnrollmentSummaryResponse summary = mock(EnrollmentSummaryResponse.class);
    CourseResponse courseResponse = mock(CourseResponse.class);
    CourseClassResponse classResponse = mock(CourseClassResponse.class);
    ClassScheduleResponse scheduleResponse = mock(ClassScheduleResponse.class);
    when(enrollments.findByStudentId_IdOrderByEnrollmentDateDesc(7)).thenReturn(List.of(enrollment));
    when(enrollments.findAccessibleCoursesByStudentId(7)).thenReturn(List.of(course));
    when(enrollments.findAccessibleClassesByStudentId(7)).thenReturn(List.of(courseClass));
    when(schedules.findAccessibleSchedulesByStudentId(7)).thenReturn(List.of(schedule));
    when(mapper.toSummaryResponse(enrollment)).thenReturn(summary);
    when(courseMapper.toResponse(course)).thenReturn(courseResponse);
    when(classMapper.toResponse(eq(courseClass), eq(0L), anyList())).thenReturn(classResponse);
    when(scheduleMapper.toResponse(schedule)).thenReturn(scheduleResponse);

    assertThat(service.getMyEnrollments(principal())).containsExactly(summary);
    assertThat(service.getMyCourses(principal())).containsExactly(courseResponse);
    assertThat(service.getMyClasses(principal())).containsExactly(classResponse);
    assertThat(service.getMySchedules(principal())).containsExactly(scheduleResponse);
  }

  @Test
  void shouldAllowAssignedTeacherAndStaffToViewClassEnrollmentsButRejectOthers() {
    Courseclass courseClass = openClass(11, 20, "3200000", 30);
    Teacher teacher = new Teacher(8);
    User teacherUser = user(80, "teacher@example.com", "TEACHER");
    teacher.setUserId(teacherUser);
    courseClass.setTeacherId(teacher);
    Enrollment enrollment = new Enrollment(1);
    EnrollmentSummaryResponse summary = mock(EnrollmentSummaryResponse.class);
    when(classes.findById(11)).thenReturn(Optional.of(courseClass));
    when(enrollments.findByCourseClassId_IdOrderByEnrollmentDateDesc(11))
        .thenReturn(List.of(enrollment));
    when(mapper.toSummaryResponse(enrollment)).thenReturn(summary);
    when(users.findByEmailIgnoreCase("teacher@example.com")).thenReturn(Optional.of(teacherUser));

    assertThat(service.getClassEnrollments(11, () -> "teacher@example.com"))
        .containsExactly(summary);

    User outsider = user(81, "other@example.com", "STUDENT");
    when(users.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(outsider));
    assertThatThrownBy(() -> service.getClassEnrollments(11, () -> "other@example.com"))
        .isInstanceOf(ForbiddenException.class);

    User admin = user(1, "admin@example.com", "ADMIN");
    when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
    assertThat(service.getClassEnrollments(11, () -> "admin@example.com"))
        .containsExactly(summary);
  }

  @Test
  void shouldReturnStaffEnrollmentOrThrowWhenItDoesNotExist() {
    Enrollment enrollment = new Enrollment(9);
    when(enrollments.findById(9)).thenReturn(Optional.of(enrollment));
    when(enrollments.findById(99)).thenReturn(Optional.empty());

    assertThat(service.getStaffEnrollment(9)).isSameAs(response);
    assertThatThrownBy(() -> service.getStaffEnrollment(99))
        .isInstanceOf(com.ntt.language_center_management.exception.ResourceNotFoundException.class);
  }

  @Test
  void shouldRejectTransferToDifferentCourseOrConflictingSchedule() {
    Student student = activeStudent(7, 70, "student@example.com");
    Courseclass source = openClass(10, 20, "100000", 30);
    source.setCourseId(new Course(1));
    Courseclass target = openClass(20, 20, "120000", 30);
    target.setCourseId(new Course(2));
    Enrollment enrollment = enrollment(15, student, source,
        EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PENDING);
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
    when(classes.lockById(10)).thenReturn(Optional.of(source));
    when(classes.lockById(20)).thenReturn(Optional.of(target));

    assertThatThrownBy(() -> service.transfer(15, new TransferEnrollmentRequest(20)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("cùng khóa học");

    target.setCourseId(source.getCourseId());
    when(enrollments.countByCourseClassId_IdAndEnrollmentStatusIn(eq(20), anySet())).thenReturn(0L);
    when(enrollments.existsScheduleConflict(eq(7), eq(20), anySet())).thenReturn(true);
    assertThatThrownBy(() -> service.transfer(15, new TransferEnrollmentRequest(20)))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("trùng thời gian");
  }

  private void mockCurrentStudent(Student student) {
    mockCurrentUser(student.getUserId());
    when(students.findByUserId_EmailIgnoreCase(student.getUserId().getEmail()))
        .thenReturn(Optional.of(student));
  }

  private void mockCurrentUser(User user) {
    when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
  }

  private Principal principal() {
    return () -> "student@example.com";
  }

  private User user(int id, String email, String roleCode) {
    User value = new User(id);
    value.setEmail(email);
    value.setStatus(AccountStatus.ACTIVE);
    value.setRoleId(new Role(id, roleCode, roleCode));
    return value;
  }

  private Student activeStudent(int studentId, int userId, String email) {
    User user = new User(userId);
    user.setEmail(email);
    user.setStatus(AccountStatus.ACTIVE);
    Student student = new Student(studentId);
    student.setUserId(user);
    return student;
  }

  private Courseclass openClass(int id, int maxStudents, String fee, int startsInDays) {
    Courseclass courseClass = new Courseclass(id);
    courseClass.setStatus(ClassStatus.OPEN);
    courseClass.setMaxStudents(maxStudents);
    courseClass.setAppliedTuitionFee(new BigDecimal(fee));
    courseClass.setStartDate(Date.from(Instant.now().plus(startsInDays, ChronoUnit.DAYS)));
    courseClass.setEndDate(Date.from(Instant.now().plus(startsInDays + 60L, ChronoUnit.DAYS)));
    if (courseClass.getCourseId() == null) courseClass.setCourseId(new Course(3));
    return courseClass;
  }

  private Enrollment enrollment(int id, Student student, Courseclass courseClass,
      EnrollmentStatus status, EnrollmentPaymentStatus paymentStatus) {
    Enrollment enrollment = new Enrollment(id);
    enrollment.setStudentId(student);
    enrollment.setCourseClassId(courseClass);
    enrollment.setEnrollmentStatus(status);
    enrollment.setPaymentStatus(paymentStatus);
    enrollment.setAmountDue(courseClass.getAppliedTuitionFee());
    enrollment.setEnrollmentDate(new Date());
    enrollment.setPaymentDeadline(Date.from(Instant.now().plus(2, ChronoUnit.DAYS)));
    return enrollment;
  }
}
