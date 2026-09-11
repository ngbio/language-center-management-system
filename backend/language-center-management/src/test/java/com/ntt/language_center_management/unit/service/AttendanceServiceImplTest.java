package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.AttendanceBulkRequest;
import com.ntt.language_center_management.dto.request.AttendanceItemRequest;
import com.ntt.language_center_management.dto.request.AttendanceUpdateRequest;
import com.ntt.language_center_management.entity.Attendance;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AttendanceStatus;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.repository.AttendanceRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.service.impl.AttendanceServiceImpl;
import java.security.Principal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AttendanceServiceImplTest {
  private AttendanceRepository attendances;
  private LessonRepository lessons;
  private EnrollmentRepository enrollments;
  private StudentRepository students;
  private TeacherRepository teachers;
  private CourseClassRepository classes;
  private AttendanceServiceImpl service;
  private Teacher teacher;
  private Courseclass courseClass;
  private Lesson lesson;
  private Enrollment enrollment;

  @BeforeEach
  void setUp() {
    attendances = mock(AttendanceRepository.class);
    lessons = mock(LessonRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    students = mock(StudentRepository.class);
    teachers = mock(TeacherRepository.class);
    classes = mock(CourseClassRepository.class);
    service = new AttendanceServiceImpl(attendances, lessons, enrollments, students, teachers,
        classes, 7, "Asia/Ho_Chi_Minh");
    teacher = teacher(3, "teacher@example.com");
    courseClass = courseClass(10, teacher);
    lesson = lesson(20, courseClass, LocalDate.now());
    enrollment = enrollment(30, student(7, "HV007", "Student A"), courseClass);
    when(lessons.findById(20)).thenReturn(Optional.of(lesson));
    when(teachers.findByUserId_EmailIgnoreCase("teacher@example.com"))
        .thenReturn(Optional.of(teacher));
    when(enrollments
        .findByCourseClassId_IdAndEnrollmentStatusAndPaymentStatusOrderByStudentId_UserId_FullNameAsc(
            10, EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PAID))
        .thenReturn(List.of(enrollment));
  }

  @Test
  void shouldRejectTeacherWhoIsNotAssignedToClass() {
    when(teachers.findByUserId_EmailIgnoreCase("other@example.com"))
        .thenReturn(Optional.of(teacher(4, "other@example.com")));

    assertThatThrownBy(() -> service.getSheet(20, principal("other@example.com")))
        .isInstanceOf(ForbiddenException.class).hasMessageContaining("không phải giảng viên");
  }

  @Test
  void shouldBuildSheetOnlyFromPaidConfirmedEnrollments() {
    var sheet = service.getSheet(20, principal("teacher@example.com"));

    assertThat(sheet.lessonId()).isEqualTo(20);
    assertThat(sheet.students()).singleElement().satisfies(item -> {
      assertThat(item.studentId()).isEqualTo(7);
      assertThat(item.status()).isNull();
    });
    verify(enrollments)
        .findByCourseClassId_IdAndEnrollmentStatusAndPaymentStatusOrderByStudentId_UserId_FullNameAsc(
            10, EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PAID);
  }

  @Test
  void shouldCreateAndUpdateBulkAttendanceWithoutOverwritingOriginalTime() {
    Student second = student(8, "HV008", "Student B");
    Enrollment secondEnrollment = enrollment(31, second, courseClass);
    when(enrollments
        .findByCourseClassId_IdAndEnrollmentStatusAndPaymentStatusOrderByStudentId_UserId_FullNameAsc(
            10, EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PAID))
        .thenReturn(List.of(enrollment, secondEnrollment));
    Date originalTime = new Date(1_000);
    Attendance existing = attendance(40, enrollment, lesson, AttendanceStatus.ABSENT);
    existing.setAttendanceTime(originalTime);
    when(attendances.findByLessonId_IdOrderByEnrollmentId_StudentId_UserId_FullNameAsc(20))
        .thenReturn(List.of(existing), List.of(existing));

    service.saveBulk(20, new AttendanceBulkRequest(List.of(
        new AttendanceItemRequest(7, AttendanceStatus.PRESENT, "  đúng giờ  "),
        new AttendanceItemRequest(8, AttendanceStatus.LATE, "  "))),
        principal("teacher@example.com"));

    ArgumentCaptor<List<Attendance>> captor = ArgumentCaptor.forClass(List.class);
    verify(attendances).saveAll(captor.capture());
    Attendance updated = captor.getValue().get(0);
    Attendance created = captor.getValue().get(1);
    assertThat(updated).isSameAs(existing);
    assertThat(updated.getAttendanceTime()).isEqualTo(originalTime);
    assertThat(updated.getUpdatedAt()).isNotNull();
    assertThat(updated.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    assertThat(updated.getNote()).isEqualTo("đúng giờ");
    assertThat(created.getAttendanceTime()).isNotNull();
    assertThat(created.getUpdatedAt()).isNull();
    assertThat(created.getNote()).isNull();
  }

  @Test
  void shouldRejectDuplicateOrUnknownStudentWithoutSavingSheet() {
    AttendanceBulkRequest duplicate = new AttendanceBulkRequest(List.of(
        new AttendanceItemRequest(7, AttendanceStatus.PRESENT, null),
        new AttendanceItemRequest(7, AttendanceStatus.ABSENT, null)));
    assertThatThrownBy(() -> service.saveBulk(20, duplicate, principal("teacher@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("bị trùng");

    AttendanceBulkRequest unknown = new AttendanceBulkRequest(List.of(
        new AttendanceItemRequest(99, AttendanceStatus.PRESENT, null)));
    assertThatThrownBy(() -> service.saveBulk(20, unknown, principal("teacher@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không thuộc lớp");
    verify(attendances, never()).saveAll(any());
  }

  @Test
  void shouldRejectCancelledLessonAndCancelledClass() {
    lesson.setStatus(LessonStatus.CANCELLED);
    assertThatThrownBy(() -> service.saveBulk(20, requestFor(7), principal("teacher@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("buổi học đã hủy");

    lesson.setStatus(LessonStatus.COMPLETED);
    courseClass.setStatus(ClassStatus.CANCELLED);
    assertThatThrownBy(() -> service.saveBulk(20, requestFor(7), principal("teacher@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("lớp học đã hủy");
  }

  @Test
  void shouldRejectAttendanceUpdateAfterEditWindow() {
    Lesson oldLesson = lesson(21, courseClass, LocalDate.now().minusDays(8));
    Attendance attendance = attendance(40, enrollment, oldLesson, AttendanceStatus.PRESENT);
    when(attendances.findById(40)).thenReturn(Optional.of(attendance));

    assertThatThrownBy(() -> service.update(40,
        new AttendanceUpdateRequest(AttendanceStatus.ABSENT, "sửa"),
        principal("teacher@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("7 ngày");
    verify(attendances, never()).save(any());
  }

  @Test
  void shouldUpdateSingleAttendanceAndPreserveMarkedTime() {
    Date originalTime = new Date(1_000);
    Attendance attendance = attendance(40, enrollment, lesson, AttendanceStatus.ABSENT);
    attendance.setAttendanceTime(originalTime);
    when(attendances.findById(40)).thenReturn(Optional.of(attendance));
    when(attendances.save(attendance)).thenReturn(attendance);

    var response = service.update(40,
        new AttendanceUpdateRequest(AttendanceStatus.EXCUSED, "  Có phép  "),
        principal("teacher@example.com"));

    assertThat(attendance.getAttendanceTime()).isEqualTo(originalTime);
    assertThat(attendance.getUpdatedAt()).isNotNull();
    assertThat(attendance.getNote()).isEqualTo("Có phép");
    assertThat(response.status()).isEqualTo("EXCUSED");
  }

  @Test
  void shouldSummarizeEveryAttendanceStatusAndAvoidDivisionByZero() {
    Enrollment emptyEnrollment = enrollment(31, student(8, "HV008", "Student B"), courseClass);
    when(classes.findById(10)).thenReturn(Optional.of(courseClass));
    when(enrollments
        .findByCourseClassId_IdAndEnrollmentStatusAndPaymentStatusOrderByStudentId_UserId_FullNameAsc(
            10, EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PAID))
        .thenReturn(List.of(enrollment, emptyEnrollment));
    when(attendances.findByClassId(10)).thenReturn(List.of(
        attendance(1, enrollment, lesson, AttendanceStatus.PRESENT),
        attendance(2, enrollment, lesson, AttendanceStatus.LATE),
        attendance(3, enrollment, lesson, AttendanceStatus.ABSENT),
        attendance(4, enrollment, lesson, AttendanceStatus.EXCUSED)));
    when(lessons.countByClassScheduleId_CourseClassId_IdAndStatusNot(10, LessonStatus.CANCELLED))
        .thenReturn(5L);
    when(lessons.countByClassScheduleId_CourseClassId_IdAndStatus(10, LessonStatus.COMPLETED))
        .thenReturn(4L);

    var summary = service.getClassSummary(10, principal("teacher@example.com"));

    assertThat(summary.totalLessons()).isEqualTo(5);
    assertThat(summary.completedLessons()).isEqualTo(4);
    assertThat(summary.students()).hasSize(2);
    assertThat(summary.students().get(0).attendanceRate()).isEqualTo(50.0);
    assertThat(summary.students().get(1).attendanceRate()).isZero();
  }

  @Test
  void shouldReturnOnlyCurrentStudentsAttendanceHistory() {
    Student current = enrollment.getStudentId();
    when(students.findByUserId_EmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(current));
    Attendance record = attendance(40, enrollment, lesson, AttendanceStatus.PRESENT);
    when(attendances.findByEnrollmentId_StudentId_IdOrderByLessonId_LessonDateDesc(7))
        .thenReturn(List.of(record));

    var result = service.getMine(principal("student@example.com"));

    assertThat(result).singleElement().satisfies(value -> {
      assertThat(value.id()).isEqualTo(40);
      assertThat(value.studentId()).isEqualTo(7);
      assertThat(value.status()).isEqualTo("PRESENT");
    });
    verify(attendances)
        .findByEnrollmentId_StudentId_IdOrderByLessonId_LessonDateDesc(7);
  }

  private AttendanceBulkRequest requestFor(int studentId) {
    return new AttendanceBulkRequest(List.of(
        new AttendanceItemRequest(studentId, AttendanceStatus.PRESENT, null)));
  }

  private Principal principal(String email) {
    return () -> email;
  }

  private Teacher teacher(int id, String email) {
    User user = new User(id + 100);
    user.setEmail(email);
    Teacher value = new Teacher(id);
    value.setUserId(user);
    return value;
  }

  private Courseclass courseClass(int id, Teacher assignedTeacher) {
    Courseclass value = new Courseclass(id);
    value.setClassCode("EN-A1-01");
    value.setClassName("English A1");
    value.setTeacherId(assignedTeacher);
    value.setStatus(ClassStatus.OPEN);
    return value;
  }

  private Lesson lesson(int id, Courseclass ownerClass, LocalDate date) {
    Classschedule schedule = new Classschedule(5);
    schedule.setCourseClassId(ownerClass);
    schedule.setStartTime(Time.valueOf(LocalTime.MIN));
    schedule.setEndTime(Time.valueOf(LocalTime.of(23, 59)));
    Lesson value = new Lesson(id);
    value.setClassScheduleId(schedule);
    value.setLessonDate(java.sql.Date.valueOf(date));
    value.setTopic("Lesson " + id);
    value.setStatus(LessonStatus.COMPLETED);
    return value;
  }

  private Student student(int id, String code, String name) {
    User user = new User(id + 200);
    user.setFullName(name);
    Student value = new Student(id);
    value.setStudentCode(code);
    value.setUserId(user);
    return value;
  }

  private Enrollment enrollment(int id, Student student, Courseclass ownerClass) {
    Enrollment value = new Enrollment(id);
    value.setStudentId(student);
    value.setCourseClassId(ownerClass);
    value.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    value.setPaymentStatus(EnrollmentPaymentStatus.PAID);
    return value;
  }

  private Attendance attendance(int id, Enrollment owner, Lesson ownerLesson,
      AttendanceStatus status) {
    Attendance value = new Attendance(id);
    value.setEnrollmentId(owner);
    value.setLessonId(ownerLesson);
    value.setStatus(status);
    value.setAttendanceTime(new Date());
    return value;
  }
}
