package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.LessonStatus;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;

import com.ntt.language_center_management.dto.request.LessonRescheduleRequest;
import com.ntt.language_center_management.dto.request.LessonUpdateRequest;
import com.ntt.language_center_management.dto.response.LessonResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.LessonMapper;
import com.ntt.language_center_management.repository.AttendanceRepository;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.LessonService;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class LessonServiceImpl implements LessonService {

  private final LessonRepository lessonRepository;
  private final ClassScheduleRepository classScheduleRepository;
  private final CourseClassRepository courseClassRepository;
  private final AttendanceRepository attendanceRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final LessonMapper lessonMapper;
  private final ZoneId applicationZone;

  public LessonServiceImpl(
      LessonRepository lessonRepository,
      ClassScheduleRepository classScheduleRepository,
      CourseClassRepository courseClassRepository,
      AttendanceRepository attendanceRepository,
      EnrollmentRepository enrollmentRepository,
      StudentRepository studentRepository,
      UserRepository userRepository,
      LessonMapper lessonMapper,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone) {
    this.lessonRepository = lessonRepository;
    this.classScheduleRepository = classScheduleRepository;
    this.courseClassRepository = courseClassRepository;
    this.attendanceRepository = attendanceRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.studentRepository = studentRepository;
    this.userRepository = userRepository;
    this.lessonMapper = lessonMapper;
    this.applicationZone = ZoneId.of(applicationTimeZone);
  }

  @Override
  public List<LessonResponse> generate(Integer classId, Principal principal) {
    Courseclass courseClass = lockClass(classId);
    User editor = ensureCanEditContent(courseClass, principal);
    if ("TEACHER".equals(editor.getRoleId().getRoleCode())
        && LocalDate.now().isBefore(toLocalDate(courseClass.getStartDate()))) {
      throw new IllegalArgumentException(
          "Giảng viên chỉ được sinh buổi học từ ngày khai giảng của lớp");
    }
    ensureClassAllowsLessonChanges(courseClass);
    List<Classschedule> schedules =
        classScheduleRepository.findByCourseClassId_IdOrderByDayOfWeekAscStartTimeAsc(classId);
    if (schedules.isEmpty()) {
      throw new IllegalArgumentException("Lớp chưa có lịch học để sinh buổi");
    }

    int totalSessions = courseClass.getCourseId().getTotalSessions();
    List<Lesson> existingLessons =
        lessonRepository.findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(classId);
    if (existingLessons.size() > totalSessions) {
      throw new IllegalArgumentException("Số buổi hiện có đã vượt tổng số buổi của khóa học");
    }
    int remaining = totalSessions - existingLessons.size();
    if (remaining == 0) {
      return existingLessons.stream().map(lessonMapper::toResponse).toList();
    }

    Set<String> existingKeys = new HashSet<>();
    for (Lesson lesson : existingLessons) {
      existingKeys.add(key(lesson.getClassScheduleId().getId(), toLocalDate(lesson.getLessonDate())));
    }

    List<Lesson> lessonsToCreate = new ArrayList<>();
    LocalDate date = toLocalDate(courseClass.getStartDate());
    LocalDate endDate = toLocalDate(courseClass.getEndDate());
    while (!date.isAfter(endDate) && lessonsToCreate.size() < remaining) {
      for (Classschedule schedule : schedules) {
        if (date.getDayOfWeek().getValue() != schedule.getDayOfWeek()
            || existingKeys.contains(key(schedule.getId(), date))) {
          continue;
        }
        validateActualConflict(null, courseClass, schedule, date);
        Lesson lesson = new Lesson();
        lesson.setClassScheduleId(schedule);
        lesson.setLessonDate(toDate(date));
        lesson.setStatus(LessonStatus.SCHEDULED);
        lessonsToCreate.add(lesson);
        existingKeys.add(key(schedule.getId(), date));
        if (lessonsToCreate.size() == remaining) {
          break;
        }
      }
      date = date.plusDays(1);
    }

    if (lessonsToCreate.size() != remaining) {
      throw new IllegalArgumentException(
          "Khoảng ngày và lịch lặp không đủ để sinh " + totalSessions + " buổi học");
    }
    lessonRepository.saveAll(lessonsToCreate);
    return lessonRepository.findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(classId)
        .stream()
        .map(lessonMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonResponse> getByClassId(Integer classId, Principal principal) {
    Courseclass courseClass = findClass(classId);
    ensureClassMember(courseClass, principal);
    return lessonRepository.findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(classId)
        .stream()
        .map(lessonMapper::toResponse)
        .toList();
  }

  @Override
  public LessonResponse update(Integer id, LessonUpdateRequest request, Principal principal) {
    Lesson lesson = lockLesson(id);
    ensureCanEditContent(lesson.getClassScheduleId().getCourseClassId(), principal);
    if (Set.of("COMPLETED", "CANCELLED").contains(lesson.getStatus())) {
      throw new IllegalArgumentException("Không thể sửa nội dung buổi học đã hoàn thành hoặc đã hủy");
    }
    lesson.setTopic(StringUtils.hasText(request.topic()) ? request.topic().trim() : null);
    return lessonMapper.toResponse(lessonRepository.save(lesson));
  }

  @Override
  public LessonResponse reschedule(Integer id, LessonRescheduleRequest request) {
    Lesson lesson = lockLesson(id);
    Courseclass courseClass = lesson.getClassScheduleId().getCourseClassId();
    if (Set.of("COMPLETED", "CANCELLED").contains(lesson.getStatus())) {
      throw new IllegalArgumentException("Không thể dời buổi học đã hoàn thành hoặc đã hủy");
    }
    ensureNoAttendance(lesson);
    ensureClassAllowsLessonChanges(courseClass);

    LocalDateTime now = LocalDateTime.now(applicationZone);
    LocalDate currentLessonDate = toLocalDate(lesson.getLessonDate());
    LocalTime lessonStartTime = toLocalTime(lesson.getClassScheduleId().getStartTime());
    if (!now.isBefore(LocalDateTime.of(currentLessonDate, lessonStartTime))) {
      throw new IllegalArgumentException("Chỉ được dời buổi học chưa bắt đầu");
    }

    LocalDate lessonDate = request.lessonDate();
    LocalDate startDate = toLocalDate(courseClass.getStartDate());
    LocalDate endDate = toLocalDate(courseClass.getEndDate());
    if (lessonDate.isBefore(startDate) || lessonDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Ngày học mới phải nằm trong khoảng thời gian của lớp");
    }
    if (lessonDate.equals(currentLessonDate)) {
      throw new IllegalArgumentException("Ngày học mới phải khác ngày học hiện tại");
    }
    if (!LocalDateTime.of(lessonDate, lessonStartTime).isAfter(now)) {
      throw new IllegalArgumentException("Ngày học mới phải là một thời điểm chưa diễn ra");
    }
    Date newDate = toDate(lessonDate);
    if (lessonRepository.existsByClassScheduleId_IdAndLessonDateAndIdNot(
        lesson.getClassScheduleId().getId(), newDate, lesson.getId())) {
      throw new DuplicateResourceException("Lịch này đã có buổi học trong ngày được chọn");
    }
    validateActualConflict(lesson.getId(), courseClass, lesson.getClassScheduleId(), lessonDate);
    if (lesson.getOriginalLessonDate() == null) {
      lesson.setOriginalLessonDate(lesson.getLessonDate());
    }
    lesson.setLessonDate(newDate);
    lesson.setRescheduleReason(request.reason().trim());
    lesson.setRescheduledAt(Date.from(now.atZone(applicationZone).toInstant()));
    // TODO(notification): notify the assigned teacher and enrolled students after commit.
    return lessonMapper.toResponse(lessonRepository.save(lesson));
  }

  @Override
  public LessonResponse cancel(Integer id) {
    Lesson lesson = lockLesson(id);
    if (lesson.getStatus() == LessonStatus.COMPLETED) {
      throw new IllegalArgumentException("Không thể hủy buổi học đã hoàn thành");
    }
    ensureNoAttendance(lesson);
    ensureClassAllowsLessonChanges(lesson.getClassScheduleId().getCourseClassId());
    if (lesson.getStatus() == LessonStatus.CANCELLED) {
      throw new IllegalArgumentException("Buổi học đã được hủy trước đó");
    }
    lesson.setStatus(LessonStatus.CANCELLED);
    return lessonMapper.toResponse(lessonRepository.save(lesson));
  }

  private void validateActualConflict(
      Integer lessonId, Courseclass courseClass, Classschedule schedule, LocalDate date) {
    Integer roomId = schedule.getRoomId() == null ? null : schedule.getRoomId().getId();
    Integer teacherId = courseClass.getTeacherId() == null ? null : courseClass.getTeacherId().getId();
    if (lessonRepository.existsResourceConflictOnDate(
        lessonId,
        courseClass.getId(),
        roomId,
        teacherId,
        toDate(date),
        schedule.getStartTime(),
        schedule.getEndTime())) {
      throw new DuplicateResourceException("Buổi học bị trùng phòng hoặc giảng viên");
    }
  }

  private void ensureClassMember(Courseclass courseClass, Principal principal) {
    User user = findUser(principal);
    String role = user.getRoleId().getRoleCode();
    if (Set.of("ADMIN", "CONSULTANT").contains(role)) {
      return;
    }
    if ("TEACHER".equals(role)
        && courseClass.getTeacherId() != null
        && courseClass.getTeacherId().getUserId().getId().equals(user.getId())) {
      return;
    }
    if ("STUDENT".equals(role)) {
      Student student =
          studentRepository
              .findByUserId_EmailIgnoreCase(user.getEmail())
              .orElseThrow(() -> new ForbiddenException("Không có hồ sơ học viên hợp lệ"));
      if (enrollmentRepository
          .existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusAndPaymentStatus(
              student.getId(), courseClass.getId(), EnrollmentStatus.CONFIRMED,
              EnrollmentPaymentStatus.PAID)) {
        return;
      }
    }
    throw new ForbiddenException("Bạn không phải thành viên của lớp học này");
  }

  private User ensureCanEditContent(Courseclass courseClass, Principal principal) {
    User user = findUser(principal);
    String role = user.getRoleId().getRoleCode();
    if (Set.of("ADMIN", "CONSULTANT").contains(role)) {
      return user;
    }
    if ("TEACHER".equals(role)
        && courseClass.getTeacherId() != null
        && courseClass.getTeacherId().getUserId().getId().equals(user.getId())) {
      return user;
    }
    throw new ForbiddenException("Bạn không được sửa nội dung buổi học này");
  }

  private User findUser(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new ForbiddenException("Không xác định được người dùng hiện tại");
    }
    return userRepository
        .findByEmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ForbiddenException("Không tìm thấy người dùng hiện tại"));
  }

  private void ensureNoAttendance(Lesson lesson) {
    if (attendanceRepository.existsByLessonId_Id(lesson.getId())) {
      throw new IllegalArgumentException("Không thể dời hoặc hủy buổi học đã có điểm danh");
    }
  }

  private void ensureClassAllowsLessonChanges(Courseclass courseClass) {
    if (courseClass.getStatus() == ClassStatus.COMPLETED
        || courseClass.getStatus() == ClassStatus.CANCELLED) {
      throw new IllegalArgumentException("Không thể thay đổi buổi học của lớp đã kết thúc hoặc đã hủy");
    }
  }

  private Courseclass findClass(Integer id) {
    return courseClassRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private Courseclass lockClass(Integer id) {
    return courseClassRepository
        .lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private Lesson findLesson(Integer id) {
    return lessonRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học"));
  }

  private Lesson lockLesson(Integer id) {
    return lessonRepository
        .lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học"));
  }

  private String key(Integer scheduleId, LocalDate date) {
    return scheduleId + ":" + date;
  }

  private Date toDate(LocalDate value) {
    return java.sql.Date.valueOf(value);
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
}
