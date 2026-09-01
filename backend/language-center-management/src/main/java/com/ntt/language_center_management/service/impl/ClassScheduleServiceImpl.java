package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Room;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.LessonRepository;
import com.ntt.language_center_management.repository.RoomRepository;
import com.ntt.language_center_management.service.ClassScheduleService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
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
  private final LessonRepository lessonRepository;
  private final RoomRepository roomRepository;
  private final ClassScheduleMapper classScheduleMapper;

  public ClassScheduleServiceImpl(
      ClassScheduleRepository classScheduleRepository,
      CourseClassRepository courseClassRepository,
      LessonRepository lessonRepository,
      RoomRepository roomRepository,
      ClassScheduleMapper classScheduleMapper) {
    this.classScheduleRepository = classScheduleRepository;
    this.courseClassRepository = courseClassRepository;
    this.lessonRepository = lessonRepository;
    this.roomRepository = roomRepository;
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
    Courseclass courseClass = findClass(classId);
    ensureClassAllowsScheduleChanges(courseClass);
    ensureLessonsNotGenerated(courseClass.getId());

    Classschedule schedule = new Classschedule();
    schedule.setCourseClassId(courseClass);
    applyRequest(schedule, request);
    validateConflicts(schedule);
    return classScheduleMapper.toResponse(classScheduleRepository.save(schedule));
  }

  @Override
  public ClassScheduleResponse update(Integer id, ClassScheduleRequest request) {
    Classschedule schedule = findSchedule(id);
    ensureClassAllowsScheduleChanges(schedule.getCourseClassId());
    ensureLessonsNotGenerated(schedule.getCourseClassId().getId());

    applyRequest(schedule, request);
    validateConflicts(schedule);
    return classScheduleMapper.toResponse(classScheduleRepository.save(schedule));
  }

  @Override
  public void delete(Integer id) {
    Classschedule schedule = findSchedule(id);
    ensureClassAllowsScheduleChanges(schedule.getCourseClassId());
    if (lessonRepository.existsByClassScheduleId_Id(id)) {
      throw new IllegalArgumentException("Không thể xóa lịch đã sinh buổi học");
    }
    classScheduleRepository.delete(schedule);
  }

  private void applyRequest(Classschedule schedule, ClassScheduleRequest request) {
    if (!request.startTime().isBefore(request.endTime())) {
      throw new IllegalArgumentException("Giờ bắt đầu phải trước giờ kết thúc");
    }

    Room room = validateLocation(request, schedule.getCourseClassId());
    schedule.setRoomId(room);
    schedule.setDayOfWeek(request.dayOfWeek());
    schedule.setStartTime(toDate(request.startTime()));
    schedule.setEndTime(toDate(request.endTime()));
    schedule.setDeliveryMode(request.deliveryMode());
    schedule.setMeetingUrl(
        StringUtils.hasText(request.meetingUrl()) ? request.meetingUrl().trim() : null);
  }

  private Room validateLocation(ClassScheduleRequest request, Courseclass courseClass) {
    if ("IN_PERSON".equals(request.deliveryMode())) {
      if (request.roomId() == null) {
        throw new IllegalArgumentException("Lịch học trực tiếp phải chọn phòng");
      }
      if (StringUtils.hasText(request.meetingUrl())) {
        throw new IllegalArgumentException("Lịch học trực tiếp không được có đường dẫn online");
      }
      Room room =
          roomRepository
              .findByIdAndStatus(request.roomId(), "ACTIVE")
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng đang hoạt động"));
      if (room.getCapacity() < courseClass.getMaxStudents()) {
        throw new IllegalArgumentException("Sức chứa phòng nhỏ hơn sĩ số tối đa của lớp");
      }
      return room;
    }

    if (request.roomId() != null) {
      throw new IllegalArgumentException("Lịch học online không được chọn phòng học");
    }
    if (!StringUtils.hasText(request.meetingUrl())) {
      throw new IllegalArgumentException("Lịch học online phải có đường dẫn phòng học");
    }
    return null;
  }

  private void validateConflicts(Classschedule schedule) {
    Courseclass courseClass = schedule.getCourseClassId();
    Integer scheduleId = schedule.getId();
    Integer roomId = schedule.getRoomId() == null ? null : schedule.getRoomId().getId();
    Integer teacherId = courseClass.getTeacherId() == null ? null : courseClass.getTeacherId().getId();

    if (classScheduleRepository.existsClassTimeConflict(
        scheduleId,
        courseClass.getId(),
        schedule.getDayOfWeek(),
        schedule.getStartTime(),
        schedule.getEndTime())) {
      throw new DuplicateResourceException("Lịch mới bị chồng thời gian với lịch khác trong lớp");
    }

    if (classScheduleRepository.existsResourceConflict(
        scheduleId,
        roomId,
        teacherId,
        courseClass.getStartDate(),
        courseClass.getEndDate(),
        schedule.getDayOfWeek(),
        schedule.getStartTime(),
        schedule.getEndTime())) {
      throw new DuplicateResourceException("Lịch học bị trùng phòng hoặc giảng viên");
    }

    LocalDate date = toLocalDate(courseClass.getStartDate());
    LocalDate endDate = toLocalDate(courseClass.getEndDate());
    while (!date.isAfter(endDate)) {
      if (date.getDayOfWeek().getValue() == schedule.getDayOfWeek()
          && lessonRepository.existsResourceConflictOnDate(
              null,
              courseClass.getId(),
              roomId,
              teacherId,
              toDate(date),
              schedule.getStartTime(),
              schedule.getEndTime())) {
        throw new DuplicateResourceException("Lịch học bị trùng với buổi học thực tế đã có");
      }
      date = date.plusDays(1);
    }
  }

  private void ensureClassAllowsScheduleChanges(Courseclass courseClass) {
    if ("COMPLETED".equals(courseClass.getStatus()) || "CANCELLED".equals(courseClass.getStatus())) {
      throw new IllegalArgumentException("Không thể thay đổi lịch của lớp đã kết thúc hoặc đã hủy");
    }
  }

  private void ensureLessonsNotGenerated(Integer classId) {
    if (lessonRepository.countByClassScheduleId_CourseClassId_Id(classId) > 0) {
      throw new IllegalArgumentException("Không thể thay đổi lịch sau khi đã sinh buổi học");
    }
  }

  private Courseclass findClass(Integer id) {
    return courseClassRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private Classschedule findSchedule(Integer id) {
    return classScheduleRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch học"));
  }

  private Date toDate(LocalTime value) {
    return Date.from(
        LocalDate.of(1970, 1, 1).atTime(value).atZone(ZoneId.systemDefault()).toInstant());
  }

  private Date toDate(LocalDate value) {
    return Date.from(value.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  private LocalDate toLocalDate(Date value) {
    return value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }
}
