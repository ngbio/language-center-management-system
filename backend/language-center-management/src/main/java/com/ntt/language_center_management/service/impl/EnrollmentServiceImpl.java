package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.CancelEnrollmentRequest;
import com.ntt.language_center_management.dto.request.CreateEnrollmentRequest;
import com.ntt.language_center_management.dto.request.TransferEnrollmentRequest;
import com.ntt.language_center_management.dto.response.EnrollmentResponse;
import com.ntt.language_center_management.dto.response.EnrollmentSummaryResponse;
import com.ntt.language_center_management.dto.response.CourseResponse;
import com.ntt.language_center_management.dto.response.CourseClassResponse;
import com.ntt.language_center_management.dto.response.ClassScheduleResponse;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.EnrollmentMapper;
import com.ntt.language_center_management.mapper.CourseMapper;
import com.ntt.language_center_management.mapper.CourseClassMapper;
import com.ntt.language_center_management.mapper.ClassScheduleMapper;
import com.ntt.language_center_management.repository.ClassScheduleRepository;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.EnrollmentService;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class EnrollmentServiceImpl implements EnrollmentService {

  private static final String PENDING = "PENDING";
  private static final String CONFIRMED = "CONFIRMED";
  private static final String CANCELLED = "CANCELLED";
  private static final Set<String> ACTIVE_STATUSES = Set.of(PENDING, CONFIRMED);
  private static final Set<String> ENROLLMENT_STATUSES =
      Set.of(PENDING, CONFIRMED, CANCELLED);

  private final EnrollmentRepository enrollmentRepository;
  private final CourseClassRepository courseClassRepository;
  private final StudentRepository studentRepository;
  private final UserRepository userRepository;
  private final EnrollmentMapper enrollmentMapper;
  private final CourseMapper courseMapper;
  private final CourseClassMapper courseClassMapper;
  private final ClassScheduleMapper classScheduleMapper;
  private final ClassScheduleRepository classScheduleRepository;

  public EnrollmentServiceImpl(
      EnrollmentRepository enrollmentRepository,
      CourseClassRepository courseClassRepository,
      StudentRepository studentRepository,
      UserRepository userRepository,
      EnrollmentMapper enrollmentMapper,
      CourseMapper courseMapper,
      CourseClassMapper courseClassMapper,
      ClassScheduleMapper classScheduleMapper,
      ClassScheduleRepository classScheduleRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.courseClassRepository = courseClassRepository;
    this.studentRepository = studentRepository;
    this.userRepository = userRepository;
    this.enrollmentMapper = enrollmentMapper;
    this.courseMapper = courseMapper;
    this.courseClassMapper = courseClassMapper;
    this.classScheduleMapper = classScheduleMapper;
    this.classScheduleRepository = classScheduleRepository;
  }

  @Override
  public EnrollmentResponse enrollMe(CreateEnrollmentRequest request, Principal principal) {
    return createEnrollment(findCurrentStudent(principal), request.courseClassId());
  }

  @Override
  public EnrollmentResponse enrollByStaff(CreateEnrollmentRequest request) {
    if (request.studentId() == null) {
      throw new IllegalArgumentException("Học viên không được để trống khi nhân viên đăng ký");
    }
    return createEnrollment(findStudent(request.studentId()), request.courseClassId());
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
    return enrollmentRepository.findAccessibleClassesByStudentId(student.getId()).stream()
        .map(
            courseClass ->
                courseClassMapper.toResponse(
                    courseClass, countActiveEnrollments(courseClass.getId())))
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
    String role = user.getRoleId().getRoleCode();

    boolean canView = Set.of("ADMIN", "CONSULTANT").contains(role);
    if ("TEACHER".equals(role)
        && courseClass.getTeacherId() != null
        && courseClass.getTeacherId().getUserId().getId().equals(user.getId())) {
      canView = true;
    }
    if (!canView) {
      throw new ForbiddenException("Bạn không được xem danh sách học viên của lớp này");
    }

    return enrollmentRepository.findByCourseClassId_IdOrderByEnrollmentDateDesc(classId).stream()
        .map(enrollmentMapper::toSummaryResponse)
        .toList();
  }

  @Override
  public EnrollmentResponse requestCancel(
      Integer enrollmentId, CancelEnrollmentRequest request, Principal principal) {
    Enrollment enrollment = lockEnrollment(enrollmentId);
    Student currentStudent = findCurrentStudent(principal);

    if (!enrollment.getStudentId().getId().equals(currentStudent.getId())) {
      throw new ForbiddenException("Bạn không được hủy đăng ký của học viên khác");
    }

    validateCancellationPolicy(enrollment);
    cancel(enrollment, request.cancellationReason());
    reopenClassIfNeeded(lockClass(enrollment.getCourseClassId().getId()));
    return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
  }

  @Override
  public EnrollmentResponse changeStatus(Integer enrollmentId, String status) {
    if (!ENROLLMENT_STATUSES.contains(status)) {
      throw new IllegalArgumentException(
          "Trạng thái đăng ký phải là PENDING, CONFIRMED hoặc CANCELLED");
    }

    Enrollment enrollment = lockEnrollment(enrollmentId);
    String currentStatus = enrollment.getEnrollmentStatus();
    if (currentStatus.equals(status)) {
      return enrollmentMapper.toResponse(enrollment);
    }
    if (CANCELLED.equals(currentStatus)) {
      throw new IllegalArgumentException("Không thể thay đổi đăng ký đã hủy");
    }
    if (CONFIRMED.equals(currentStatus) && PENDING.equals(status)) {
      throw new IllegalArgumentException("Không thể chuyển đăng ký đã xác nhận về chờ xử lý");
    }

    Courseclass courseClass = lockClass(enrollment.getCourseClassId().getId());
    if (CANCELLED.equals(status)) {
      validateCancellationPolicy(enrollment);
      cancel(enrollment, "Hủy bởi nhân viên");
      reopenClassIfNeeded(courseClass);
    } else {
      if (!Set.of("OPEN", "FULL").contains(courseClass.getStatus())) {
        throw new IllegalArgumentException("Lớp học không còn nhận xử lý đăng ký");
      }
      enrollment.setEnrollmentStatus(status);
      if (CONFIRMED.equals(status)) {
        enrollment.setConfirmedAt(new Date());
      }
    }
    return enrollmentMapper.toResponse(enrollmentRepository.save(enrollment));
  }

  @Override
  public EnrollmentResponse transfer(Integer enrollmentId, TransferEnrollmentRequest request) {
    Enrollment enrollment = lockEnrollment(enrollmentId);
    validateActiveStudent(enrollment.getStudentId());
    if (!PENDING.equals(enrollment.getEnrollmentStatus())
        || !PENDING.equals(enrollment.getPaymentStatus())) {
      throw new IllegalArgumentException(
          "Chỉ được chuyển lớp cho đăng ký đang chờ xử lý và chưa thanh toán");
    }
    validateTransferPolicy(enrollment.getCourseClassId());

    Integer sourceClassId = enrollment.getCourseClassId().getId();
    Integer targetClassId = request.targetCourseClassId();
    if (sourceClassId.equals(targetClassId)) {
      throw new IllegalArgumentException("Lớp chuyển đến phải khác lớp hiện tại");
    }

    Courseclass[] lockedClasses = lockClassesInOrder(sourceClassId, targetClassId);
    Courseclass sourceClass = findLockedClass(lockedClasses, sourceClassId);
    Courseclass targetClass = findLockedClass(lockedClasses, targetClassId);

    validateOpenClassAndCapacity(targetClass);
    if (enrollmentRepository.existsByStudentId_IdAndCourseClassId_Id(
        enrollment.getStudentId().getId(), targetClassId)) {
      throw new DuplicateResourceException("Học viên đã từng đăng ký lớp chuyển đến");
    }
    validateNoScheduleConflict(enrollment.getStudentId().getId(), targetClassId);

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
    validateActiveStudent(student);
    Courseclass courseClass = lockClass(courseClassId);
    validateOpenClassAndCapacity(courseClass);
    if (enrollmentRepository.existsByStudentId_IdAndCourseClassId_Id(
        student.getId(), courseClassId)) {
      throw new DuplicateResourceException("Học viên đã từng đăng ký lớp học này");
    }
    validateNoScheduleConflict(student.getId(), courseClassId);

    Enrollment enrollment = new Enrollment();
    enrollment.setStudentId(student);
    enrollment.setCourseClassId(courseClass);
    enrollment.setEnrollmentDate(new Date());
    enrollment.setAmountDue(courseClass.getAppliedTuitionFee());
    enrollment.setEnrollmentStatus(PENDING);
    enrollment.setPaymentStatus(PENDING);

    try {
      Enrollment saved = enrollmentRepository.saveAndFlush(enrollment);
      markFullIfNeeded(courseClass);
      return enrollmentMapper.toResponse(saved);
    } catch (DataIntegrityViolationException exception) {
      throw new DuplicateResourceException("Học viên đã đăng ký lớp học này");
    }
  }

  private void validateOpenClassAndCapacity(Courseclass courseClass) {
    if (!"OPEN".equals(courseClass.getStatus())) {
      throw new IllegalArgumentException("Lớp học hiện không mở đăng ký");
    }
    if (countActiveEnrollments(courseClass.getId()) >= courseClass.getMaxStudents()) {
      throw new IllegalArgumentException("Lớp học đã đủ số lượng học viên");
    }
  }

  private void validateNoScheduleConflict(Integer studentId, Integer courseClassId) {
    if (enrollmentRepository.existsScheduleConflict(
        studentId, courseClassId, ACTIVE_STATUSES)) {
      throw new IllegalArgumentException("Lớp học bị trùng thời gian với đăng ký hiện tại");
    }
  }

  private void validateCancellationPolicy(Enrollment enrollment) {
    Courseclass courseClass = enrollment.getCourseClassId();
    if (!Set.of("OPEN", "FULL").contains(courseClass.getStatus())) {
      throw new IllegalArgumentException("Không thể hủy đăng ký khi lớp đã bắt đầu hoặc kết thúc");
    }
    if (!courseClass.getStartDate().after(new Date())) {
      throw new IllegalArgumentException("Đã quá thời hạn hủy đăng ký trước ngày khai giảng");
    }
    if (!PENDING.equals(enrollment.getPaymentStatus())) {
      throw new IllegalArgumentException(
          "Đăng ký đã phát sinh thanh toán, cần xử lý hoàn tiền trước khi hủy");
    }
  }

  private void validateTransferPolicy(Courseclass sourceClass) {
    if (!Set.of("OPEN", "FULL").contains(sourceClass.getStatus())) {
      throw new IllegalArgumentException("Không thể chuyển khi lớp hiện tại đã bắt đầu hoặc kết thúc");
    }
    if (!sourceClass.getStartDate().after(new Date())) {
      throw new IllegalArgumentException("Đã quá thời hạn chuyển lớp trước ngày khai giảng");
    }
  }

  private void validateActiveStudent(Student student) {
    if (student.getUserId() == null || !"ACTIVE".equals(student.getUserId().getStatus())) {
      throw new IllegalArgumentException("Tài khoản học viên không ở trạng thái ACTIVE");
    }
  }

  private void markFullIfNeeded(Courseclass courseClass) {
    if (countActiveEnrollments(courseClass.getId()) >= courseClass.getMaxStudents()) {
      courseClass.setStatus("FULL");
    }
  }

  private void reopenClassIfNeeded(Courseclass courseClass) {
    if ("FULL".equals(courseClass.getStatus())
        && countActiveEnrollments(courseClass.getId()) < courseClass.getMaxStudents()) {
      courseClass.setStatus("OPEN");
    }
  }

  private void cancel(Enrollment enrollment, String reason) {
    if (CANCELLED.equals(enrollment.getEnrollmentStatus())) {
      throw new IllegalArgumentException("Đăng ký đã được hủy trước đó");
    }
    enrollment.setEnrollmentStatus(CANCELLED);
    enrollment.setCancelledAt(new Date());
    enrollment.setCancellationReason(reason.trim());
    if (PENDING.equals(enrollment.getPaymentStatus())) {
      enrollment.setPaymentStatus(CANCELLED);
    }
  }

  private long countActiveEnrollments(Integer courseClassId) {
    return enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(
        courseClassId, ACTIVE_STATUSES);
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

  private Student findStudent(Integer id) {
    return studentRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
  }

  private Student findCurrentStudent(Principal principal) {
    User user = findCurrentUser(principal);
    if (!"ACTIVE".equals(user.getStatus())) {
      throw new ForbiddenException("Tài khoản học viên không ở trạng thái ACTIVE");
    }
    Student student =
        studentRepository
            .findByUserId_EmailIgnoreCase(user.getEmail())
            .orElseThrow(() -> new ForbiddenException("Tài khoản chưa có hồ sơ học viên"));
    validateActiveStudent(student);
    return student;
  }

  private User findCurrentUser(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) {
      throw new ForbiddenException("Không xác định được người dùng hiện tại");
    }
    return userRepository
        .findByEmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ForbiddenException("Không tìm thấy người dùng hiện tại"));
  }

  private Courseclass findClass(Integer id) {
    return courseClassRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
  }
}
