package com.ntt.language_center_management.service.impl;

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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class LessonServiceImpl implements LessonService {
  private static final Set<String> ACTIVE_ENROLLMENTS = Set.of("PENDING", "CONFIRMED");

  private final LessonRepository lessonRepository;
  private final ClassScheduleRepository classScheduleRepository;
  private final CourseClassRepository courseClassRepository;
  private final AttendanceRepository attendanceRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final LessonMapper lessonMapper;

  public LessonServiceImpl(
      LessonRepository lessonRepository,
      ClassScheduleRepository classScheduleRepository,
      CourseClassRepository courseClassRepository,
      AttendanceRepository attendanceRepository,
      EnrollmentRepository enrollmentRepository,
      StudentRepository studentRepository,
      UserRepository userRepository,
      LessonMapper lessonMapper) {
    this.lessonRepository = lessonRepository;
    this.classScheduleRepository = classScheduleRepository;
    this.courseClassRepository = courseClassRepository;
    this.attendanceRepository = attendanceRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.studentRepository = studentRepository;
    this.userRepository = userRepository;
    this.lessonMapper = lessonMapper;
  }

  @Override
  public List<LessonResponse> generate(Integer classId) {
    Courseclass courseClass = findClass(classId);
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
        lesson.setStatus("SCHEDULED");
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
    Lesson lesson = findLesson(id);
    ensureCanEditContent(lesson.getClassScheduleId().getCourseClassId(), principal);
    if ("CANCELLED".equals(lesson.getStatus())) {
      throw new IllegalArgumentException("Không thể sửa nội dung buổi học đã hủy");
    }
    lesson.setTopic(StringUtils.hasText(request.topic()) ? request.topic().trim() : null);
    lesson.setMeetingUrl(
        StringUtils.hasText(request.meetingUrl()) ? request.meetingUrl().trim() : null);
    return lessonMapper.toResponse(lessonRepository.save(lesson));
  }

  @Override
  public LessonResponse reschedule(Integer id, LessonRescheduleRequest request) {
    Lesson lesson = findLesson(id);
    ensureNoAttendance(lesson);
    Courseclass courseClass = lesson.getClassScheduleId().getCourseClassId();
    ensureClassAllowsLessonChanges(courseClass);

    LocalDate lessonDate = request.lessonDate();
    LocalDate startDate = toLocalDate(courseClass.getStartDate());
    LocalDate endDate = toLocalDate(courseClass.getEndDate());
    if (lessonDate.isBefore(startDate) || lessonDate.isAfter(endDate)) {
      throw new IllegalArgumentException("Ngày học mới phải nằm trong khoảng thời gian của lớp");
    }
    Date newDate = toDate(lessonDate);
    if (lessonRepository.existsByClassScheduleId_IdAndLessonDateAndIdNot(
        lesson.getClassScheduleId().getId(), newDate, lesson.getId())) {
      throw new DuplicateResourceException("Lịch này đã có buổi học trong ngày được chọn");
    }
    validateActualConflict(lesson.getId(), courseClass, lesson.getClassScheduleId(), lessonDate);
    lesson.setLessonDate(newDate);
    return lessonMapper.toResponse(lessonRepository.save(lesson));
  }

  @Override
  public LessonResponse cancel(Integer id) {
    Lesson lesson = findLesson(id);
    ensureNoAttendance(lesson);
    ensureClassAllowsLessonChanges(lesson.getClassScheduleId().getCourseClassId());
    if ("CANCELLED".equals(lesson.getStatus())) {
      throw new IllegalArgumentException("Buổi học đã được hủy trước đó");
    }
    lesson.setStatus("CANCELLED");
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
      if (enrollmentRepository.existsByStudentId_IdAndCourseClassId_IdAndEnrollmentStatusIn(
          student.getId(), courseClass.getId(), ACTIVE_ENROLLMENTS)) {
        return;
      }
    }
    throw new ForbiddenException("Bạn không phải thành viên của lớp học này");
  }

  private void ensureCanEditContent(Courseclass courseClass, Principal principal) {
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
    if ("COMPLETED".equals(courseClass.getStatus()) || "CANCELLED".equals(courseClass.getStatus())) {
      throw new IllegalArgumentException("Không thể thay đổi buổi học của lớp đã kết thúc hoặc đã hủy");
    }
  }

  private Courseclass findClass(Integer id) {
    return courseClassRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private Lesson findLesson(Integer id) {
    return lessonRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy buổi học"));
  }

  private String key(Integer scheduleId, LocalDate date) {
    return scheduleId + ":" + date;
  }

  private Date toDate(LocalDate value) {
    return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private LocalDate toLocalDate(Date value) {
    return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
