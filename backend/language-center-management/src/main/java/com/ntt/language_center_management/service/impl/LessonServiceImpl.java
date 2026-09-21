package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.policy.LessonChangePolicy;
import com.ntt.language_center_management.policy.LessonAccessPolicy;
import com.ntt.language_center_management.enums.LessonStatus;

import com.ntt.language_center_management.dto.request.LessonRescheduleRequest;
import com.ntt.language_center_management.dto.request.LessonUpdateRequest;
import com.ntt.language_center_management.dto.response.LessonResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Lesson;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.LessonMapper;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.service.LessonService;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
  private final LessonMapper lessonMapper;
  private final ZoneId applicationZone;

  private final LessonAccessPolicy accessPolicy;

  private final LessonChangePolicy changePolicy;

  public LessonServiceImpl(
      LessonRepository lessonRepository,
      ClassScheduleRepository classScheduleRepository,
      CourseClassRepository courseClassRepository,
      LessonMapper lessonMapper,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone,
      LessonAccessPolicy accessPolicy,
      LessonChangePolicy changePolicy) {
    this.changePolicy = changePolicy;
    this.accessPolicy = accessPolicy;
    this.lessonRepository = lessonRepository;
    this.classScheduleRepository = classScheduleRepository;
    this.courseClassRepository = courseClassRepository;
    this.lessonMapper = lessonMapper;
    this.applicationZone = ZoneId.of(applicationTimeZone);
  }

  @Override
  public List<LessonResponse> generate(Integer classId, Principal principal) {
    Courseclass courseClass = lockClass(classId);
    User editor = accessPolicy.ensureCanEditContent(courseClass, principal);
    changePolicy.validateGenerationStart(courseClass, editor, LocalDate.now(applicationZone), applicationZone);
    changePolicy.ensureClassAllowsLessonChanges(courseClass);
    List<Classschedule> schedules =
        classScheduleRepository.findByCourseClassId_IdOrderByDayOfWeekAscStartTimeAsc(classId);
    changePolicy.validateSchedulesPresent(schedules);
    int totalSessions = courseClass.getCourseId().getTotalSessions();
    List<Lesson> existingLessons =
        lessonRepository.findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(classId);
    changePolicy.validateExistingCount(existingLessons.size(), totalSessions);
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
        changePolicy.validateActualConflict(null, courseClass, schedule, date);
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

    changePolicy.validateGeneratedCount(lessonsToCreate.size(), remaining, totalSessions);
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
    accessPolicy.ensureClassMember(courseClass, principal);
    return lessonRepository.findByClassScheduleId_CourseClassId_IdOrderByLessonDateAsc(classId)
        .stream()
        .map(lessonMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<LessonResponse> getMyLessons(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new ForbiddenException("Không xác định được học viên hiện tại");
    }
    return lessonRepository.findAccessibleLessonsByStudentEmail(principal.getName()).stream()
        .map(lessonMapper::toResponse)
        .toList();
  }

  @Override
  public LessonResponse update(Integer id, LessonUpdateRequest request, Principal principal) {
    Lesson lesson = lockLesson(id);
    accessPolicy.ensureCanEditContent(lesson.getClassScheduleId().getCourseClassId(), principal);
    changePolicy.validateContentUpdate(lesson);
    lesson.setTopic(StringUtils.hasText(request.topic()) ? request.topic().trim() : null);
    return lessonMapper.toResponse(lessonRepository.save(lesson));
  }

  @Override
  public LessonResponse reschedule(Integer id, LessonRescheduleRequest request) {
    Lesson lesson = lockLesson(id);
    LocalDateTime now = LocalDateTime.now(applicationZone);
    changePolicy.validateReschedule(lesson, request, now, applicationZone);
    Date newDate = toDate(request.lessonDate());
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
    changePolicy.validateCancellation(lesson);
    lesson.setStatus(LessonStatus.CANCELLED);
    return lessonMapper.toResponse(lessonRepository.save(lesson));
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
    return ApplicationDateTimeUtils.toLocalDate(value, applicationZone);
  }

}
