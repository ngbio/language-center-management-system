package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.AttendanceStatus;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;

import com.ntt.language_center_management.dto.request.AttendanceBulkRequest;
import com.ntt.language_center_management.dto.request.AttendanceItemRequest;
import com.ntt.language_center_management.dto.request.AttendanceUpdateRequest;
import com.ntt.language_center_management.dto.response.AttendanceResponse;
import com.ntt.language_center_management.dto.response.AttendanceSheetItemResponse;
import com.ntt.language_center_management.dto.response.AttendanceSheetResponse;
import com.ntt.language_center_management.dto.response.ClassAttendanceSummaryResponse;
import com.ntt.language_center_management.dto.response.StudentAttendanceSummaryResponse;
import com.ntt.language_center_management.entity.Attendance;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.repository.AttendanceRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.service.AttendanceService;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class AttendanceServiceImpl implements AttendanceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(AttendanceServiceImpl.class);

  private final AttendanceRepository attendanceRepository;
  private final LessonRepository lessonRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final StudentRepository studentRepository;
  private final TeacherRepository teacherRepository;
  private final CourseClassRepository courseClassRepository;
  private final int editWindowDays;
  private final ZoneId applicationZone;

  public AttendanceServiceImpl(
      AttendanceRepository attendanceRepository,
      LessonRepository lessonRepository,
      EnrollmentRepository enrollmentRepository,
      StudentRepository studentRepository,
      TeacherRepository teacherRepository,
      CourseClassRepository courseClassRepository,
      @Value("${attendance.edit-window-days:7}") int editWindowDays,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone) {
    this.attendanceRepository = attendanceRepository;
    this.lessonRepository = lessonRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.studentRepository = studentRepository;
    this.teacherRepository = teacherRepository;
    this.courseClassRepository = courseClassRepository;
    this.editWindowDays = editWindowDays;
    this.applicationZone = ZoneId.of(applicationTimeZone);
  }

  @Override
  @Transactional(readOnly = true)
  public AttendanceSheetResponse getSheet(Integer lessonId, Principal principal) {
    Lesson lesson = findLesson(lessonId);
    requireAssignedTeacher(lesson.getClassScheduleId().getCourseClassId(), principal);
    return buildSheet(lesson);
  }

  @Override
  public AttendanceSheetResponse saveBulk(
      Integer lessonId, AttendanceBulkRequest request, Principal principal) {
    Lesson lesson = findLesson(lessonId);
    Teacher teacher = requireAssignedTeacher(lesson.getClassScheduleId().getCourseClassId(), principal);
    Set<Integer> studentIds = new HashSet<>();
    for (AttendanceItemRequest item : request.attendances()) {
      if (!studentIds.add(item.studentId())) {
        throw new IllegalArgumentException("Danh sách điểm danh chứa học viên bị trùng");
      }
    }

    Map<Integer, Enrollment> validEnrollments = new HashMap<>();
    for (Enrollment enrollment : validEnrollments(lesson.getClassScheduleId().getCourseClassId().getId())) {
      validEnrollments.put(enrollment.getStudentId().getId(), enrollment);
    }
    if (!validEnrollments.keySet().containsAll(studentIds)) {
      throw new IllegalArgumentException("Có học viên không thuộc lớp hoặc chưa thanh toán hợp lệ");
    }

    ensureCanUpdateAttendance(lesson);

    Date markedAt = new Date();
    Map<Integer, Attendance> existingByStudent = new HashMap<>();
    attendanceRepository
        .findByLessonId_IdOrderByEnrollmentId_StudentId_UserId_FullNameAsc(lessonId)
        .forEach(value -> existingByStudent.put(value.getEnrollmentId().getStudentId().getId(), value));
    List<Attendance> changes = new ArrayList<>();
    for (AttendanceItemRequest item : request.attendances()) {
      Attendance attendance = existingByStudent.getOrDefault(item.studentId(), new Attendance());
      boolean isNew = attendance.getId() == null;
      attendance.setLessonId(lesson);
      attendance.setEnrollmentId(validEnrollments.get(item.studentId()));
      attendance.setStatus(item.status());
      attendance.setNote(trimToNull(item.note()));
      if (isNew) {
        attendance.setAttendanceTime(markedAt);
      } else {
        attendance.setUpdatedAt(markedAt);
      }
      changes.add(attendance);
    }
    attendanceRepository.saveAll(changes);
    LOGGER.info(
        "Teacher {} saved attendance for lesson {} ({} students)",
        teacher.getUserId().getEmail(), lessonId, changes.size());
    return buildSheet(lesson);
  }

  @Override
  public AttendanceResponse update(
      Integer attendanceId, AttendanceUpdateRequest request, Principal principal) {
    Attendance attendance = findAttendance(attendanceId);
    Lesson lesson = attendance.getLessonId();
    Teacher teacher = requireAssignedTeacher(lesson.getClassScheduleId().getCourseClassId(), principal);
    ensureCanUpdateAttendance(lesson);
    attendance.setStatus(request.status());
    attendance.setNote(trimToNull(request.note()));
    attendance.setUpdatedAt(new Date());
    Attendance saved = attendanceRepository.save(attendance);
    LOGGER.info(
        "Teacher {} updated attendance {} for lesson {}",
        teacher.getUserId().getEmail(), attendanceId, lesson.getId());
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AttendanceResponse> getMine(Principal principal) {
    Student student =
        studentRepository
            .findByUserId_EmailIgnoreCase(principalName(principal))
            .orElseThrow(() -> new ForbiddenException("Không có hồ sơ học viên hợp lệ"));
    return attendanceRepository
        .findByEnrollmentId_StudentId_IdOrderByLessonId_LessonDateDesc(student.getId())
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ClassAttendanceSummaryResponse getClassSummary(Integer classId, Principal principal) {
    Courseclass courseClass =
        courseClassRepository
            .findById(classId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
    requireAssignedTeacher(courseClass, principal);
    List<Enrollment> enrollments = validEnrollments(classId);
    List<Attendance> records =
        attendanceRepository.findByClassId(classId);

    Map<Integer, List<Attendance>> byStudent = new HashMap<>();
    records.forEach(
        attendance ->
            byStudent
                .computeIfAbsent(attendance.getEnrollmentId().getStudentId().getId(), key -> new ArrayList<>())
                .add(attendance));
    List<StudentAttendanceSummaryResponse> students =
        enrollments.stream()
            .map(enrollment -> summarize(enrollment.getStudentId(), byStudent.getOrDefault(enrollment.getStudentId().getId(), List.of())))
            .toList();
    return new ClassAttendanceSummaryResponse(
        courseClass.getId(),
        courseClass.getClassCode(),
        courseClass.getClassName(),
        lessonRepository.countByClassScheduleId_CourseClassId_IdAndStatusNot(
            classId, LessonStatus.CANCELLED),
        lessonRepository.countByClassScheduleId_CourseClassId_IdAndStatus(
            classId, LessonStatus.COMPLETED),
        students);
  }

  private AttendanceSheetResponse buildSheet(Lesson lesson) {
    Map<Integer, Attendance> existing = new HashMap<>();
    attendanceRepository.findByLessonId_IdOrderByEnrollmentId_StudentId_UserId_FullNameAsc(lesson.getId())
        .forEach(value -> existing.put(value.getEnrollmentId().getStudentId().getId(), value));
    List<AttendanceSheetItemResponse> students =
        validEnrollments(lesson.getClassScheduleId().getCourseClassId().getId()).stream()
            .map(
                enrollment -> {
                  Student student = enrollment.getStudentId();
                  Attendance attendance = existing.get(student.getId());
                  return new AttendanceSheetItemResponse(
                      attendance == null ? null : attendance.getId(),
                      student.getId(),
                      student.getStudentCode(),
                      student.getUserId().getFullName(),
                      attendance == null ? null : attendance.getStatus().name(),
                      attendance == null ? null : attendance.getNote(),
                      attendance == null ? null : attendance.getAttendanceTime());
                })
            .toList();
    Courseclass courseClass = lesson.getClassScheduleId().getCourseClassId();
    return new AttendanceSheetResponse(
        lesson.getId(), lesson.getLessonDate(), lesson.getTopic(), lesson.getStatus().name(),
        courseClass.getId(), courseClass.getClassCode(), courseClass.getClassName(), students);
  }

  private StudentAttendanceSummaryResponse summarize(Student student, List<Attendance> records) {
    long present = count(records, AttendanceStatus.PRESENT);
    long absent = count(records, AttendanceStatus.ABSENT);
    long late = count(records, AttendanceStatus.LATE);
    long excused = count(records, AttendanceStatus.EXCUSED);
    long total = records.size();
    double rate = total == 0 ? 0 : Math.round(((present + late) * 10000.0) / total) / 100.0;
    return new StudentAttendanceSummaryResponse(
        student.getId(), student.getStudentCode(), student.getUserId().getFullName(),
        total, present, absent, late, excused, rate);
  }

  private long count(List<Attendance> records, AttendanceStatus status) {
    return records.stream().filter(value -> value.getStatus() == status).count();
  }

  private List<Enrollment> validEnrollments(Integer classId) {
    return enrollmentRepository
        .findByCourseClassId_IdAndEnrollmentStatusAndPaymentStatusOrderByStudentId_UserId_FullNameAsc(
            classId, EnrollmentStatus.CONFIRMED, EnrollmentPaymentStatus.PAID);
  }

  private Teacher requireAssignedTeacher(Courseclass courseClass, Principal principal) {
    Teacher teacher =
        teacherRepository
            .findByUserId_EmailIgnoreCase(principalName(principal))
            .orElseThrow(() -> new ForbiddenException("Chỉ giảng viên mới được quản lý điểm danh"));
    if (courseClass.getTeacherId() == null || !courseClass.getTeacherId().getId().equals(teacher.getId())) {
      throw new ForbiddenException("Bạn không phải giảng viên phụ trách lớp học này");
    }
    return teacher;
  }

  private void ensureAttendanceAllowed(Lesson lesson) {
    if (lesson.getStatus() == LessonStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể điểm danh buổi học đã hủy");
    }
    if (lesson.getClassScheduleId().getCourseClassId().getStatus() == ClassStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể điểm danh cho lớp học đã hủy");
    }
  }

  private void ensureCanUpdateAttendance(Lesson lesson) {
    ensureAttendanceAllowed(lesson);
    LocalDate lessonDate = toLocalDate(lesson.getLessonDate());
    LocalDateTime now = LocalDateTime.now(applicationZone);
    LocalDateTime lessonStart =
        LocalDateTime.of(lessonDate, toLocalTime(lesson.getClassScheduleId().getStartTime()));
    if (now.isBefore(lessonStart)) {
      throw new IllegalArgumentException("Chỉ được điểm danh sau thời gian bắt đầu buổi học");
    }
    if (now.toLocalDate().isAfter(lessonDate.plusDays(editWindowDays))) {
      throw new IllegalArgumentException(
          "Đã quá thời hạn sửa điểm danh " + editWindowDays + " ngày sau buổi học");
    }
  }

  private AttendanceResponse toResponse(Attendance attendance) {
    Enrollment enrollment = attendance.getEnrollmentId();
    Student student = enrollment.getStudentId();
    Lesson lesson = attendance.getLessonId();
    Courseclass courseClass = enrollment.getCourseClassId();
    return new AttendanceResponse(
        attendance.getId(), lesson.getId(), lesson.getLessonDate(), lesson.getTopic(),
        courseClass.getId(), courseClass.getClassCode(), courseClass.getClassName(),
        student.getId(), student.getStudentCode(), student.getUserId().getFullName(),
        attendance.getStatus().name(), attendance.getNote(), attendance.getAttendanceTime());
  }

  private Lesson findLesson(Integer id) {
    return lessonRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học"));
  }

  private Attendance findAttendance(Integer id) {
    return attendanceRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy bản ghi điểm danh"));
  }

  private String principalName(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new ForbiddenException("Không xác định được người dùng hiện tại");
    }
    return principal.getName();
  }

  private LocalDate toLocalDate(Date value) {
    if (value instanceof java.sql.Date sqlDate) {
      return sqlDate.toLocalDate();
    }
    return value.toInstant().atZone(applicationZone).toLocalDate();
  }

  private LocalTime toLocalTime(Date value) {
    if (value instanceof java.sql.Time sqlTime) {
      return sqlTime.toLocalTime();
    }
    return value.toInstant().atZone(applicationZone).toLocalTime();
  }

  private String trimToNull(String value) {
    return StringUtils.hasText(value) ? value.trim() : null;
  }
}
