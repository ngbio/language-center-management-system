package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.CancelEnrollmentRequest;
import com.ntt.language_center_management.dto.request.CreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.StaffCreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.TransferEnrollmentRequest;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.entity.Classschedule;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.factory.EnrollmentFactory;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.mapper.CourseClassMapper;
import com.ntt.language_center_management.mapper.CourseMapper;
import com.ntt.language_center_management.mapper.EnrollmentMapper;
import com.ntt.language_center_management.policy.EnrollmentAccessPolicy;
import com.ntt.language_center_management.policy.EnrollmentCancellationPolicy;
import com.ntt.language_center_management.policy.EnrollmentEligibilityPolicy;
import com.ntt.language_center_management.policy.EnrollmentTransferPolicy;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.EnrollmentLifecycle;
import com.ntt.language_center_management.service.EnrollmentService;
import com.ntt.language_center_management.validation.EnrollmentValidationContext;
import com.ntt.language_center_management.validation.EnrollmentValidationPipelines;
import java.security.Principal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import static com.ntt.language_center_management.policy.EnrollmentPolicy.CAPACITY_RESERVED_STATUSES;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

  private static final Set<String> ENROLLMENT_SORT_FIELDS =
      Set.of("id", "enrollmentDate", "paymentDeadline", "amountDue", "enrollmentStatus", "paymentStatus");

  private final EnrollmentRepository enrollmentRepository;
  private final CourseClassRepository courseClassRepository;
  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final EnrollmentMapper enrollmentMapper;
  private final CourseMapper courseMapper;
  private final CourseClassMapper courseClassMapper;
  private final ClassScheduleMapper classScheduleMapper;
  private final ClassScheduleRepository classScheduleRepository;
  private final CurrentUserResolver currentUserResolver;
  private final EnrollmentAccessPolicy accessPolicy;
  private final EnrollmentEligibilityPolicy eligibilityPolicy;
  private final EnrollmentCancellationPolicy cancellationPolicy;
  private final EnrollmentTransferPolicy transferPolicy;
  private final EnrollmentFactory enrollmentFactory;
  private final EnrollmentLifecycle lifecycle;
  private final EnrollmentValidationPipelines pipelines;

  public EnrollmentServiceImpl(
      EnrollmentRepository enrollmentRepository,
      CourseClassRepository courseClassRepository,
      StudentRepository studentRepository,
      UserRepository userRepository,
      EnrollmentMapper enrollmentMapper,
      CourseMapper courseMapper,
      CourseClassMapper courseClassMapper,
      ClassScheduleMapper classScheduleMapper,
      ClassScheduleRepository classScheduleRepository,
      CurrentUserResolver currentUserResolver,
      EnrollmentAccessPolicy accessPolicy,
      EnrollmentEligibilityPolicy eligibilityPolicy,
      EnrollmentCancellationPolicy cancellationPolicy,
      EnrollmentTransferPolicy transferPolicy,
      EnrollmentFactory enrollmentFactory, EnrollmentLifecycle lifecycle, EnrollmentValidationPipelines pipelines) {
    this.enrollmentRepository = enrollmentRepository;
    this.courseClassRepository = courseClassRepository;
    this.studentRepository = studentRepository;
    this.userRepository = userRepository;
    this.enrollmentMapper = enrollmentMapper;
    this.courseMapper = courseMapper;
    this.courseClassMapper = courseClassMapper;
    this.classScheduleMapper = classScheduleMapper;
    this.classScheduleRepository = classScheduleRepository;
    this.currentUserResolver = currentUserResolver;
    this.accessPolicy = accessPolicy;
    this.eligibilityPolicy = eligibilityPolicy;
    this.cancellationPolicy = cancellationPolicy;
    this.transferPolicy = transferPolicy;
    this.enrollmentFactory = enrollmentFactory;
    this.lifecycle = lifecycle;
    this.pipelines = pipelines;

  }

  @Override
  public EnrollmentResponse enrollMe(CreateEnrollmentRequest request, Principal principal) {
    return createEnrollment(findCurrentStudent(principal), request.courseClassId());
  }

  @Override
  public EnrollmentResponse enrollByStaff(StaffCreateEnrollmentRequest request) {
    Student student = studentRepository
        .findByUserId_EmailIgnoreCase(request.studentEmail().trim())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Không tìm thấy tài khoản học viên với email " + request.studentEmail().trim()));
    return createEnrollment(student, request.courseClassId());
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<EnrollmentResponse> searchStaffEnrollments(
      String keyword, Integer courseId, Integer classId, String enrollmentStatus,
      String paymentStatus, int page, int size, String sort, String direction) {
    if (page < 0) throw new IllegalArgumentException("Số trang không được nhỏ hơn 0");
    if (size < 1 || size > 100) throw new IllegalArgumentException("Kích thước trang phải từ 1 đến 100");
    EnrollmentStatus selectedStatus = parseEnrollmentStatus(enrollmentStatus);
    EnrollmentPaymentStatus selectedPayment = parsePaymentStatus(paymentStatus);
    Specification<Enrollment> spec = (root, query, builder) -> builder.conjunction();
    if (StringUtils.hasText(keyword)) {
      String value = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
      spec = spec.and((root, query, builder) -> builder.or(
          builder.like(builder.lower(root.get("studentId").get("studentCode")), value),
          builder.like(builder.lower(root.get("studentId").get("userId").get("fullName")), value),
          builder.like(builder.lower(root.get("studentId").get("userId").get("email")), value),
          builder.like(builder.lower(root.get("courseClassId").get("classCode")), value),
          builder.like(builder.lower(root.get("courseClassId").get("className")), value)));
    }
    if (courseId != null) spec = spec.and((root, query, builder) ->
        builder.equal(root.get("courseClassId").get("courseId").get("id"), courseId));
    if (classId != null) spec = spec.and((root, query, builder) ->
        builder.equal(root.get("courseClassId").get("id"), classId));
    if (selectedStatus != null) spec = spec.and((root, query, builder) ->
        builder.equal(root.get("enrollmentStatus"), selectedStatus));
    if (selectedPayment != null) spec = spec.and((root, query, builder) ->
        builder.equal(root.get("paymentStatus"), selectedPayment));
    String sortField = ENROLLMENT_SORT_FIELDS.contains(sort) ? sort : "enrollmentDate";
    Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    return PageResponse.from(enrollmentRepository.findAll(spec,
        PageRequest.of(page, size, Sort.by(sortDirection, sortField))).map(enrollmentMapper::toResponse));
  }

  @Override
  @Transactional(readOnly = true)
  public EnrollmentResponse getStaffEnrollment(Integer id) {
    return enrollmentMapper.toResponse(enrollmentRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký học")));
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentSummaryResponse> getMyEnrollments(Principal principal) {
    Student student = findCurrentStudent(principal);
    return enrollmentRepository.findByStudentId_IdOrderByEnrollmentDateDesc(student.getId()).stream()
        .map(enrollmentMapper::toSummaryResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseResponse> getMyCourses(Principal principal) {
    Student student = findCurrentStudent(principal);
    return enrollmentRepository.findAccessibleCoursesByStudentId(student.getId()).stream()
        .map(courseMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CourseClassResponse> getMyClasses(Principal principal) {
    Student student = findCurrentStudent(principal);
    List<Courseclass> classes = enrollmentRepository.findAccessibleClassesByStudentId(student.getId());
    if (classes.isEmpty()) {
      return List.of();
    }
    List<Integer> classIds = classes.stream().map(Courseclass::getId).toList();
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
    return classes.stream()
        .map(courseClass -> courseClassMapper.toResponse(
            courseClass,
            enrollmentCounts.getOrDefault(courseClass.getId(), 0L),
            schedulesByClass.getOrDefault(courseClass.getId(), List.of()).stream()
                .map(classScheduleMapper::toResponse)
                .toList()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<ClassScheduleResponse> getMySchedules(Principal principal) {
    Student student = findCurrentStudent(principal);
    return classScheduleRepository.findAccessibleSchedulesByStudentId(student.getId()).stream()
        .map(classScheduleMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<EnrollmentSummaryResponse> getClassEnrollments(
      Integer classId, Principal principal) {
    Courseclass courseClass = findClass(classId);
    User user = findCurrentUser(principal);
    accessPolicy.requireClassRosterAccess(courseClass, user);

    return enrollmentRepository.findByCourseClassId_IdOrderByEnrollmentDateDesc(classId).stream()
        .map(enrollmentMapper::toSummaryResponse)
        .toList();
  }

  @Override
  public EnrollmentResponse requestCancel(
      Integer enrollmentId, CancelEnrollmentRequest request, Principal principal) {
    Enrollment enrollment = lockEnrollment(enrollmentId);
    Student currentStudent = findCurrentStudent(principal);

    accessPolicy.requireOwner(enrollment, currentStudent);

    cancellationPolicy.validate(enrollment);
    lifecycle.cancel(enrollment, request.cancellationReason());
    reopenClassIfNeeded(lockClass(enrollment.getCourseClassId().getId()));
    return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
  }

  @Override
  public EnrollmentResponse changeStatus(Integer enrollmentId, EnrollmentStatus requestedStatus) {

    Enrollment enrollment = lockEnrollment(enrollmentId);
    EnrollmentStatus currentStatus = enrollment.getEnrollmentStatus();
    if (currentStatus == requestedStatus) {
      return enrollmentMapper.toResponse(enrollment);
    }
    lifecycle.validateTransition(enrollment, requestedStatus);

    Courseclass courseClass = lockClass(enrollment.getCourseClassId().getId());
    if (requestedStatus == EnrollmentStatus.CANCELLED) {
      cancellationPolicy.validate(enrollment);
      lifecycle.cancel(enrollment, "Hủy bởi nhân viên");
      reopenClassIfNeeded(courseClass);
    } else {
      lifecycle.confirm(enrollment, courseClass);
    }
    return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
  }

  @Override
  public EnrollmentResponse transfer(Integer enrollmentId, TransferEnrollmentRequest request) {
    Enrollment enrollment = lockEnrollment(enrollmentId);
    eligibilityPolicy.validateActiveStudent(enrollment.getStudentId());
    transferPolicy.validateSource(enrollment);

    Integer sourceClassId = enrollment.getCourseClassId().getId();
    Integer targetClassId = request.targetCourseClassId();
    transferPolicy.validateDifferentClass(sourceClassId, targetClassId);

    Courseclass[] lockedClasses = lockClassesInOrder(sourceClassId, targetClassId);
    Courseclass sourceClass = findLockedClass(lockedClasses, sourceClassId);
    Courseclass targetClass = findLockedClass(lockedClasses, targetClassId);

    pipelines.validateTransferTarget(EnrollmentValidationContext.transfer(
        enrollment.getStudentId(), sourceClass, targetClass, enrollment.getId()));

    enrollment.setCourseClassId(targetClass);
    enrollment.setAmountDue(targetClass.getAppliedTuitionFee());
    try {
      Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
      reopenClassIfNeeded(sourceClass);
      markFullIfNeeded(targetClass);
      return enrollmentMapper.toResponse(saved);
    } catch (DataIntegrityViolationException exception) {
      throw new DuplicateResourceException("Học viên đã đăng ký lớp chuyển đến");
    }
  }

  private EnrollmentResponse createEnrollment(Student student, Integer courseClassId) {
    eligibilityPolicy.validateActiveStudent(student);
    Courseclass courseClass = lockClass(courseClassId);
    pipelines.validateRegistrationTarget(EnrollmentValidationContext.registration(student, courseClass));

    Enrollment enrollment = enrollmentFactory.createConfirmed(student, courseClass);

    try {
      Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
      markFullIfNeeded(courseClass);
      return enrollmentMapper.toResponse(saved);
    } catch (DataIntegrityViolationException exception) {
      throw new DuplicateResourceException("Học viên đã đăng ký lớp học này");
    }
  }

  private void markFullIfNeeded(Courseclass courseClass) {
    if (countActiveEnrollments(courseClass.getId()) >= courseClass.getMaxStudents()) {
      courseClass.setStatus(ClassStatus.FULL);
    }
  }

  private void reopenClassIfNeeded(Courseclass courseClass) {
    if (courseClass.getStatus() == ClassStatus.FULL
        && countActiveEnrollments(courseClass.getId()) < courseClass.getMaxStudents()) {
      courseClass.setStatus(ClassStatus.OPEN);
    }
  }

  private long countActiveEnrollments(Integer courseClassId) {
    return enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(
        courseClassId, CAPACITY_RESERVED_STATUSES);
  }

  private Courseclass[] lockClassesInOrder(Integer firstId, Integer secondId) {
    Integer lowerId = Math.min(firstId, secondId);
    Integer higherId = Math.max(firstId, secondId);
    return new Courseclass[] {lockClass(lowerId), lockClass(higherId)};
  }

  private Courseclass findLockedClass(Courseclass[] classes, Integer id) {
    return classes[0].getId().equals(id) ? classes[0] : classes[1];
  }

  private Enrollment lockEnrollment(Integer id) {
    return enrollmentRepository
        .lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký học"));
  }

  private Courseclass lockClass(Integer id) {
    return courseClassRepository
        .lockById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private Student findCurrentStudent(Principal principal) {
    User user = findCurrentUser(principal);
    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new ForbiddenException("Tài khoản học viên không ở trạng thái ACTIVE");
    }
    Student student =
        studentRepository
            .findByUserId_EmailIgnoreCase(user.getEmail())
            .orElseThrow(() -> new ForbiddenException("Tài khoản chưa có hồ sơ học viên"));
    eligibilityPolicy.validateActiveStudent(student);
    return student;
  }

  private User findCurrentUser(Principal principal) {
    return currentUserResolver.requireUser(principal);
  }

  private Courseclass findClass(Integer id) {
    return courseClassRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }

  private EnrollmentStatus parseEnrollmentStatus(String value) {
    if (!StringUtils.hasText(value)) return null;
    try { return EnrollmentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
    catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Trạng thái đăng ký không hợp lệ"); }
  }

  private EnrollmentPaymentStatus parsePaymentStatus(String value) {
    if (!StringUtils.hasText(value)) return null;
    try { return EnrollmentPaymentStatus.valueOf(value.trim().toUpperCase(Locale.ROOT)); }
    catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Trạng thái thanh toán không hợp lệ"); }
  }
}
