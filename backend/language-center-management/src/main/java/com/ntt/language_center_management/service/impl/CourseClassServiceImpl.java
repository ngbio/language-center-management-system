package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.enums.PublicationStatus;


import com.ntt.language_center_management.dto.request.CourseClassRequest;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Teacher;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.CourseClassMapper;
import com.ntt.language_center_management.mapper.CourseMapper;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.CourseRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.service.CourseClassService;
import com.ntt.language_center_management.event.ClassOpenedMailEvent;
import com.ntt.language_center_management.util.ApplicationDateTimeUtils;
import com.ntt.language_center_management.security.CurrentUserResolver;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import static com.ntt.language_center_management.policy.EnrollmentPolicy.CAPACITY_RESERVED_STATUSES;

@Service
@Transactional
public class CourseClassServiceImpl implements CourseClassService {

  private static final Set<String> SORT_FIELDS =
      Set.of("classCode", "className", "startDate", "endDate", "appliedTuitionFee", "createdAt");
  private static final Map<ClassStatus, Set<ClassStatus>> TRANSITIONS =
      Map.of(
          ClassStatus.DRAFT, Set.of(ClassStatus.OPEN, ClassStatus.CANCELLED),
          ClassStatus.OPEN, Set.of(ClassStatus.FULL, ClassStatus.IN_PROGRESS, ClassStatus.CANCELLED),
          ClassStatus.FULL, Set.of(ClassStatus.OPEN, ClassStatus.IN_PROGRESS,
              ClassStatus.COMPLETED, ClassStatus.CANCELLED),
          ClassStatus.IN_PROGRESS, Set.of(ClassStatus.COMPLETED, ClassStatus.CANCELLED),
          ClassStatus.COMPLETED, Set.of(),
          ClassStatus.CANCELLED, Set.of());

  private final CourseClassRepository courseClassRepository;
  private final CourseRepository courseRepository;
  private final TeacherRepository teacherRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final ClassScheduleRepository classScheduleRepository;
  private final CourseClassMapper courseClassMapper;
  private final CourseMapper courseMapper;
  private final ClassScheduleMapper classScheduleMapper;
  private final ZoneId applicationZone;
  private final CurrentUserResolver currentUserResolver;
  private final ApplicationEventPublisher eventPublisher;

  public CourseClassServiceImpl(
      CourseClassRepository courseClassRepository,
      CourseRepository courseRepository,
      TeacherRepository teacherRepository,
      EnrollmentRepository enrollmentRepository,
      ClassScheduleRepository classScheduleRepository,
      CourseClassMapper courseClassMapper,
      CourseMapper courseMapper,
      ClassScheduleMapper classScheduleMapper,
      @Value("${app.time-zone:Asia/Ho_Chi_Minh}") String applicationTimeZone,
      CurrentUserResolver currentUserResolver,
      ApplicationEventPublisher eventPublisher) {
    this.courseClassRepository = courseClassRepository;
    this.courseRepository = courseRepository;
    this.teacherRepository = teacherRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.classScheduleRepository = classScheduleRepository;
    this.courseClassMapper = courseClassMapper;
    this.courseMapper = courseMapper;
    this.classScheduleMapper = classScheduleMapper;
    this.applicationZone = ZoneId.of(applicationTimeZone);
    this.currentUserResolver = currentUserResolver;
    this.eventPublisher = eventPublisher;
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<CourseClassResponse> searchOpenClasses(
      String keyword,
      Integer courseId,
      Integer levelId,
      Date date,
      int page,
      int size,
      String sort,
      String direction) {
    Specification<Courseclass> spec = (root, query, cb) -> cb.equal(root.get("status"), "OPEN");
    spec = spec.and((root, query, cb) -> cb.and(
        cb.equal(root.get("courseId").get("status"), "ACTIVE"),
        cb.equal(root.get("courseId").get("publicationStatus"), "PUBLISHED")));

    if (StringUtils.hasText(keyword)) {
      String value = "%" + keyword.trim().toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("classCode")), value),
                      cb.like(cb.lower(root.get("className")), value)));
    }
    if (courseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("courseId").get("id"), courseId));
    }
    if (levelId != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.equal(root.get("courseId").get("levelId").get("id"), levelId));
    }
    if (date != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("startDate"), date));
    }

    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    String sortField = SORT_FIELDS.contains(sort) ? sort : "startDate";
    Sort.Direction sortDirection =
        "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

    var result =
        courseClassRepository
            .findAll(spec, PageRequest.of(safePage, safeSize, Sort.by(sortDirection, sortField)));
    return pageResponse(result, false);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<CourseClassResponse> searchAdminClasses(
      String keyword,
      Integer courseId,
      Integer levelId,
      String status,
      int page,
      int size,
      String sort,
      String direction) {
    Specification<Courseclass> spec = (root, query, cb) -> cb.conjunction();

    if (StringUtils.hasText(keyword)) {
      String value = "%" + keyword.trim().toLowerCase() + "%";
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.or(
                      cb.like(cb.lower(root.get("classCode")), value),
                      cb.like(cb.lower(root.get("className")), value)));
    }
    if (courseId != null) {
      spec = spec.and((root, query, cb) -> cb.equal(root.get("courseId").get("id"), courseId));
    }
    if (levelId != null) {
      spec =
          spec.and(
              (root, query, cb) ->
                  cb.equal(root.get("courseId").get("levelId").get("id"), levelId));
    }
    if (StringUtils.hasText(status)) {
      ClassStatus normalizedStatus;
      try {
        normalizedStatus = ClassStatus.valueOf(status.trim().toUpperCase());
      } catch (IllegalArgumentException exception) {
        throw new IllegalArgumentException("Trạng thái lớp không hợp lệ");
      }
      spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), normalizedStatus));
    }

    int safePage = Math.max(page, 0);
    int safeSize = Math.min(Math.max(size, 1), 100);
    String sortField = SORT_FIELDS.contains(sort) ? sort : "startDate";
    Sort.Direction sortDirection =
        "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC : Sort.Direction.ASC;

    var result =
        courseClassRepository
            .findAll(spec, PageRequest.of(safePage, safeSize, Sort.by(sortDirection, sortField)));
    return pageResponse(result, true);
  }

  @Override
  @Transactional(readOnly = true)
  public CourseClassResponse getById(Integer id) {
    Courseclass value = find(id);
    if (value.getStatus() != ClassStatus.OPEN
        || value.getCourseId().getStatus() != CatalogStatus.ACTIVE
        || value.getCourseId().getPublicationStatus() != PublicationStatus.PUBLISHED) {
      throw new ResourceNotFoundException("Không tìm thấy lớp học đang mở");
    }
    return toResponse(value, false);
  }

  @Override
  @Transactional(readOnly = true)
  public CourseClassResponse getAdminById(Integer id) {
    return toResponse(find(id), true);
  }

  @Override
  public CourseClassResponse create(CourseClassRequest request) {
    validateCode(request.getClassCode(), null);
    validateDates(request);
    Courseclass value = new Courseclass();
    applyRequest(value, request);
    value.setStatus(ClassStatus.DRAFT);
    Date now = new Date();
    value.setCreatedAt(now);
    value.setUpdatedAt(now);
    return toResponse(courseClassRepository.save(value));
  }

  @Override
  public CourseClassResponse update(Integer id, CourseClassRequest request) {
    Courseclass value = lock(id);
    validateCode(request.getClassCode(), id);
    validateDates(request);
    long enrolled = countActiveEnrollments(id);
    if (request.getMaxStudents() < enrolled) {
      throw new IllegalArgumentException("Sĩ số tối đa không được nhỏ hơn số đăng ký hiện tại");
    }
    applyRequest(value, request);
    value.setUpdatedAt(new Date());
    if (isActiveClass(value.getStatus())) {
      validateSchedulesAndConflicts(value);
    }
    return toResponse(courseClassRepository.save(value));
  }

  @Override
  public void deleteDraft(Integer id) {
    Courseclass value = lock(id);
    if (value.getStatus() != ClassStatus.DRAFT) {
      throw new IllegalArgumentException("Chỉ có thể xóa lớp ở trạng thái DRAFT");
    }
    if (enrollmentRepository.existsByCourseClassId_Id(id)) {
      throw new IllegalArgumentException("Không thể xóa lớp đã có lịch sử đăng ký");
    }
    courseClassRepository.delete(value);
  }

  @Override
  public CourseClassResponse assignTeacher(Integer id, Integer teacherId) {
    Courseclass value = lock(id);
    value.setTeacherId(findActiveTeacher(teacherId));
    value.setUpdatedAt(new Date());
    if (isActiveClass(value.getStatus())) {
      validateSchedulesAndConflicts(value);
    }
    return toResponse(courseClassRepository.save(value));
  }

  @Override
  public CourseClassResponse changeStatus(Integer id, ClassStatus status) {
    Courseclass value = lock(id);
    ClassStatus previousStatus = value.getStatus();
    if (status == null) {
      throw new IllegalArgumentException("Trạng thái lớp không được để trống");
    }
    ClassStatus normalizedStatus = status;
    if (!TRANSITIONS.getOrDefault(value.getStatus(), Set.of()).contains(normalizedStatus)) {
      throw new IllegalArgumentException(
          "Không thể chuyển trạng thái từ " + value.getStatus() + " sang " + normalizedStatus);
    }
    if (previousStatus == ClassStatus.DRAFT && normalizedStatus == ClassStatus.OPEN) {
      validateCanOpen(value);
    }
    if (normalizedStatus == ClassStatus.FULL
        && countActiveEnrollments(id) < value.getMaxStudents()) {
      throw new IllegalArgumentException("Chỉ có thể chuyển FULL khi lớp đã đủ sĩ số");
    }
    value.setStatus(normalizedStatus);
    value.setUpdatedAt(new Date());
    Courseclass saved = courseClassRepository.save(value);
    if (normalizedStatus == ClassStatus.OPEN) {
      eventPublisher.publishEvent(new ClassOpenedMailEvent(
          saved.getClassCode(), saved.getClassName(), saved.getStartDate(),
          saved.getAppliedTuitionFee()));
    }
    return toResponse(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseClassResponse> getTeacherClasses(Principal principal) {
    Teacher teacher = findTeacherByPrincipal(principal);
    return toResponses(
        courseClassRepository.findByTeacherId_IdOrderByStartDateDesc(teacher.getId()), true);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseResponse> getTeacherCourses(Principal principal) {
    Teacher teacher = findTeacherByPrincipal(principal);
    return courseClassRepository.findDistinctCoursesByTeacherId(teacher.getId()).stream()
        .map(courseMapper::toResponse)
        .toList();
  }

  private Teacher findTeacherByPrincipal(Principal principal) {
    return currentUserResolver.requireTeacher(principal);
  }

  private void applyRequest(Courseclass value, CourseClassRequest request) {
    var course =
        courseRepository
            .findByIdAndStatus(request.getCourseId(), CatalogStatus.ACTIVE)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy khóa học đang hoạt động"));
    Teacher teacher =
        request.getTeacherId() == null
            ? value.getTeacherId()
            : findActiveTeacher(request.getTeacherId());
    if (teacher != null) {
      ensureTeacherActive(teacher);
    }
    courseClassMapper.updateEntity(value, request, course, teacher);
  }

  private void validateCode(String code, Integer id) {
    String normalized = code.trim().toUpperCase();
    boolean exists =
        id == null
            ? courseClassRepository.existsByClassCodeIgnoreCase(normalized)
            : courseClassRepository.existsByClassCodeIgnoreCaseAndIdNot(normalized, id);
    if (exists) {
      throw new DuplicateResourceException("Mã lớp đã tồn tại");
    }
  }

  private void validateDates(CourseClassRequest request) {
    if (request.getMaxStudents() < 1) {
      throw new IllegalArgumentException("Sĩ số tối đa phải lớn hơn 0");
    }
    if (request.getAppliedTuitionFee() == null
        || request.getAppliedTuitionFee().signum() < 0) {
      throw new IllegalArgumentException("Học phí áp dụng không được âm");
    }
    if (!request.getStartDate().before(request.getEndDate())) {
      throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc");
    }
  }

  private void validateCanOpen(Courseclass value) {
    if (value.getCourseId().getStatus() != CatalogStatus.ACTIVE) {
      throw new IllegalArgumentException("Khóa học không hoạt động");
    }
    if (value.getTeacherId() == null) {
      throw new IllegalArgumentException("Phải phân công giảng viên trước khi mở lớp");
    }
    ensureTeacherActive(value.getTeacherId());
    if (toLocalDate(value.getStartDate()).isBefore(LocalDate.now(applicationZone))) {
      throw new IllegalArgumentException("Không thể mở lớp đã qua ngày bắt đầu");
    }
    validateSchedulesAndConflicts(value);
  }

  private void validateSchedulesAndConflicts(Courseclass value) {
    List<Classschedule> schedules = classScheduleRepository.findByCourseClassId_Id(value.getId());
    if (schedules.isEmpty()) {
      throw new IllegalArgumentException("Lớp phải có ít nhất một lịch học hợp lệ");
    }
    for (Classschedule schedule : schedules) {
      if (schedule.getDayOfWeek() < 1
          || schedule.getDayOfWeek() > 7
          || !schedule.getStartTime().before(schedule.getEndTime())) {
        throw new IllegalArgumentException("Lịch học của lớp không hợp lệ");
      }
      Integer roomId = schedule.getRoomId() == null ? null : schedule.getRoomId().getId();
      Integer teacherId = value.getTeacherId() == null ? null : value.getTeacherId().getId();
      if (classScheduleRepository.existsConflict(
          value.getId(),
          roomId,
          teacherId,
          value.getStartDate(),
          value.getEndDate(),
          schedule.getDayOfWeek(),
          schedule.getStartTime(),
          schedule.getEndTime())) {
        throw new IllegalArgumentException("Lịch học bị trùng phòng hoặc giảng viên");
      }
    }
    validateInternalScheduleConflicts(schedules);
  }

  private void validateInternalScheduleConflicts(List<Classschedule> schedules) {
    for (int i = 0; i < schedules.size(); i++) {
      Classschedule first = schedules.get(i);
      for (int j = i + 1; j < schedules.size(); j++) {
        Classschedule second = schedules.get(j);
        boolean sameDay = first.getDayOfWeek() == second.getDayOfWeek();
        boolean overlap =
            first.getStartTime().before(second.getEndTime())
                && first.getEndTime().after(second.getStartTime());
        if (sameDay && overlap) {
          throw new IllegalArgumentException("Các lịch trong cùng lớp bị chồng thời gian");
        }
      }
    }
  }

  private Teacher findActiveTeacher(Integer id) {
    Teacher teacher =
        teacherRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giảng viên"));
    ensureTeacherActive(teacher);
    return teacher;
  }

  private void ensureTeacherActive(Teacher teacher) {
    if (teacher.getUserId().getStatus() != AccountStatus.ACTIVE) {
      throw new IllegalArgumentException("Giảng viên không hoạt động");
    }
  }

  private boolean isActiveClass(ClassStatus status) {
    return Set.of(ClassStatus.OPEN, ClassStatus.FULL, ClassStatus.IN_PROGRESS).contains(status);
  }

  private Courseclass find(Integer id) {
    return courseClassRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private Courseclass lock(Integer id) {
    return courseClassRepository
        .lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private long countActiveEnrollments(Integer id) {
    if (id == null) {
      return 0;
    }
    return enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(
        id, CAPACITY_RESERVED_STATUSES);
  }

  private CourseClassResponse toResponse(Courseclass value) {
    return courseClassMapper.toResponse(value, countActiveEnrollments(value.getId()));
  }

  private CourseClassResponse toResponse(Courseclass value, boolean includeMeetingUrl) {
    List<Classschedule> schedules = value.getId() == null
        ? List.of()
        : classScheduleRepository.findByCourseClassId_IdOrderByDayOfWeekAscStartTimeAsc(value.getId());
    return courseClassMapper.toResponse(
        value,
        countActiveEnrollments(value.getId()),
        schedules.stream()
            .map(includeMeetingUrl
                ? classScheduleMapper::toResponse
                : classScheduleMapper::toPublicResponse)
            .toList());
  }

  private PageResponse<CourseClassResponse> pageResponse(
      org.springframework.data.domain.Page<Courseclass> page,
      boolean includeMeetingUrl) {
    return new PageResponse<>(
        toResponses(page.getContent(), includeMeetingUrl),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.isFirst(),
        page.isLast());
  }

  private List<CourseClassResponse> toResponses(
      List<Courseclass> values,
      boolean includeMeetingUrl) {
    if (values.isEmpty()) {
      return List.of();
    }
    List<Integer> classIds = values.stream()
        .map(Courseclass::getId)
        .filter(java.util.Objects::nonNull)
        .toList();
    Map<Integer, Long> enrollmentCounts = enrollmentRepository
        .countByCourseClassIdsAndEnrollmentStatusIn(classIds, CAPACITY_RESERVED_STATUSES)
        .stream()
        .collect(Collectors.toMap(
            count -> count.getCourseClassId(),
            count -> count.getEnrollmentCount()));
    Map<Integer, List<Classschedule>> schedulesByClass = classScheduleRepository
        .findByCourseClassId_IdInOrderByCourseClassId_IdAscDayOfWeekAscStartTimeAsc(classIds)
        .stream()
        .collect(Collectors.groupingBy(schedule -> schedule.getCourseClassId().getId()));

    return values.stream()
        .map(value -> courseClassMapper.toResponse(
            value,
            enrollmentCounts.getOrDefault(value.getId(), 0L),
            schedulesByClass.getOrDefault(value.getId(), List.of()).stream()
                .map(includeMeetingUrl
                    ? classScheduleMapper::toResponse
                    : classScheduleMapper::toPublicResponse)
                .toList()))
        .toList();
  }

  private LocalDate toLocalDate(Date date) {
    return ApplicationDateTimeUtils.toLocalDate(date, applicationZone);
  }
}
