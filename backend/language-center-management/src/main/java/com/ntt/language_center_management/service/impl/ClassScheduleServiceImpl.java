package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.policy.ScheduleLocationPolicy;
import com.ntt.language_center_management.policy.ScheduleConflictChecker;
import com.ntt.language_center_management.policy.ScheduleChangePolicy;

import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Room;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.service.ClassScheduleService;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ClassScheduleServiceImpl implements ClassScheduleService {
  private final ClassScheduleRepository classScheduleRepository;
  private final CourseClassRepository courseClassRepository;
  private final ClassScheduleMapper classScheduleMapper;

  private final ScheduleLocationPolicy locationPolicy;

  private final ScheduleChangePolicy changePolicy;

  private final ScheduleConflictChecker conflictChecker;

  public ClassScheduleServiceImpl(
      ClassScheduleRepository classScheduleRepository,
      CourseClassRepository courseClassRepository,
      ClassScheduleMapper classScheduleMapper,
      ScheduleLocationPolicy locationPolicy,
      ScheduleChangePolicy changePolicy,
      ScheduleConflictChecker conflictChecker) {
    this.conflictChecker = conflictChecker;
    this.changePolicy = changePolicy;
    this.locationPolicy = locationPolicy;
    this.classScheduleRepository = classScheduleRepository;
    this.courseClassRepository = courseClassRepository;
    this.classScheduleMapper = classScheduleMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClassScheduleResponse> getByClassId(Integer classId) {
    findClass(classId);
    return classScheduleRepository
        .findByCourseClassId_IdOrderByDayOfWeekAscStartTimeAsc(classId)
        .stream()
        .map(classScheduleMapper::toResponse)
        .toList();
  }

  @Override
  public ClassScheduleResponse create(Integer classId, ClassScheduleRequest request) {
    Courseclass courseClass = lockClass(classId);
    changePolicy.ensureClassAllowsScheduleChanges(courseClass);
    changePolicy.ensureLessonsNotGenerated(courseClass.getId());

    Classschedule schedule = new Classschedule();
    schedule.setCourseClassId(courseClass);
    applyRequest(schedule, request);
    conflictChecker.validateConflicts(schedule);
    return classScheduleMapper.toResponse(classScheduleRepository.save(schedule));
  }

  @Override
  public ClassScheduleResponse update(Integer id, ClassScheduleRequest request) {
    Classschedule schedule = lockSchedule(id);
    Courseclass courseClass = lockClass(schedule.getCourseClassId().getId());
    schedule.setCourseClassId(courseClass);
    changePolicy.ensureClassAllowsScheduleChanges(courseClass);
    changePolicy.ensureLessonsNotGenerated(courseClass.getId());

    applyRequest(schedule, request);
    conflictChecker.validateConflicts(schedule);
    return classScheduleMapper.toResponse(classScheduleRepository.save(schedule));
  }

  @Override
  public void delete(Integer id) {
    Classschedule schedule = lockSchedule(id);
    Courseclass courseClass = lockClass(schedule.getCourseClassId().getId());
    schedule.setCourseClassId(courseClass);
    changePolicy.ensureClassAllowsScheduleChanges(courseClass);
    changePolicy.validateDeletion(id);
    classScheduleRepository.delete(schedule);
  }

  private void applyRequest(Classschedule schedule, ClassScheduleRequest request) {
    changePolicy.validateTimes(request);

    Room room = locationPolicy.validateLocation(request, schedule.getCourseClassId());
    schedule.setRoomId(room);
    schedule.setDayOfWeek(request.dayOfWeek());
    schedule.setStartTime(toDate(request.startTime()));
    schedule.setEndTime(toDate(request.endTime()));
    schedule.setDeliveryMode(request.deliveryMode());
    schedule.setMeetingUrl(
        StringUtils.hasText(request.meetingUrl()) ? request.meetingUrl().trim() : null);
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

  private Classschedule lockSchedule(Integer id) {
    return classScheduleRepository
        .lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));
  }

  private Date toDate(LocalTime value) {
    return java.sql.Time.valueOf(value);
  }

}
