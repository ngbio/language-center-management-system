package com.ntt.language_center_management.integration.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.ntt.language_center_management.dto.request.*;
import com.ntt.language_center_management.mapper.*;
import com.ntt.language_center_management.policy.*;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.EnrollmentLifecycle;
import com.ntt.language_center_management.service.impl.EnrollmentServiceImpl;
import com.ntt.language_center_management.validation.EnrollmentValidationPipelines;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.util.Optional;
import static com.ntt.language_center_management.policy.EnrollmentPolicy.CAPACITY_RESERVED_STATUSES;

import com.ntt.language_center_management.entity.*;
import com.ntt.language_center_management.enums.*;
import com.ntt.language_center_management.factory.EnrollmentFactory;
import com.ntt.language_center_management.policy.EnrollmentEligibilityPolicy;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.Clock;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

/** Runs the real JPQL against an isolated in-memory database; never uses developer credentials. */
@SpringJUnitConfig(EnrollmentTransferQueryTest.Config.class)
@Transactional
@org.junit.jupiter.api.TestInstance(org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS)
class EnrollmentTransferQueryTest {
  @Configuration
  @EnableTransactionManagement
  @EnableJpaRepositories(basePackageClasses = EnrollmentRepository.class)
  static class Config {
    @Bean DataSource dataSource() {
      var source = new JdbcDataSource();
      source.setURL("jdbc:h2:mem:transfer-" + UUID.randomUUID() + ";MODE=MySQL;NON_KEYWORDS=USER;DB_CLOSE_DELAY=-1");
      return source;
    }
    @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
      var factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(source);
      factory.setPackagesToScan("com.ntt.language_center_management.entity");
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      var properties = new Properties();
      properties.setProperty("hibernate.hbm2ddl.auto", "create-drop");
      factory.setJpaProperties(properties);
      return factory;
    }
    @Bean PlatformTransactionManager transactionManager(jakarta.persistence.EntityManagerFactory factory) {
      return new JpaTransactionManager(factory);
    }
  }

  @PersistenceContext EntityManager entityManager;
  @Autowired EnrollmentRepository enrollments;
  @Autowired NotificationRepository notifications;
  @Autowired CourseClassRepository classes;
  @Autowired StudentRepository students;
  @Autowired DataSource dataSource;
  private Student student;
  private Course course;
  private Courseclass target;
  private Enrollment source;

  private java.sql.Connection schemaConnection;

  @org.junit.jupiter.api.BeforeAll
  void installActiveEnrollmentConstraint() throws Exception {
    // Keep this connection until teardown: H2's optimized IN expression retains
    // its creation session. Production uses VARCHAR, unlike Hibernate's H2 enum.
    schemaConnection = dataSource.getConnection();
    try (var statement = schemaConnection.createStatement()) {
      statement.execute("ALTER TABLE enrollment ADD COLUMN active_slot INT "
          + "GENERATED ALWAYS AS (CASE WHEN CAST(enrollment_status AS VARCHAR) IN ('PENDING', 'CONFIRMED') THEN 1 ELSE NULL END)");
      statement.execute("CREATE UNIQUE INDEX uq_enrollment_student_class_active "
          + "ON enrollment(student_id, course_class_id, active_slot)");
    }
  }

  @org.junit.jupiter.api.AfterAll
  void closeSchemaConnection() throws Exception {
    if (schemaConnection != null) schemaConnection.close();
  }

  @BeforeEach
  void fixtures() {
    var role = persist(new Role(null, "STUDENT", "Học viên"));
    var user = new User(null, "student", "test-hash", "Học viên", "student@example.com", AccountStatus.ACTIVE, new Date());
    user.setRoleId(role);
    persist(user);
    student = new Student(null, "S001");
    student.setUserId(user);
    persist(student);
    var language = persist(new Language(null, "EN", "English", CatalogStatus.ACTIVE));
    var level = new Level(null, "A1", "Beginner", 1, CatalogStatus.ACTIVE);
    level.setLanguageId(language);
    persist(level);
    course = new Course();
    course.setCourseCode("EN-A1"); course.setCourseName("English A1"); course.setSlug("en-a1");
    course.setTuitionFee(BigDecimal.TEN); course.setTotalSessions(10);
    course.setStatus(CatalogStatus.ACTIVE); course.setCreatedAt(new Date()); course.setLevelId(level);
    persist(course);
    source = enrollment(scheduledClass("SOURCE"), EnrollmentStatus.CONFIRMED);
    target = scheduledClass("TARGET");
    entityManager.flush();
  }

  @Test
  void overlappingSourceIsExcludedOnlyForTransfer() {
    assertThat(enrollments.existsScheduleConflict(student.getId(), target.getId(), CAPACITY_RESERVED_STATUSES)).isTrue();
    assertThat(enrollments.existsScheduleConflictExcludingEnrollment(
        student.getId(), source.getId(), target.getId(), CAPACITY_RESERVED_STATUSES)).isFalse();
    assertThatCode(() -> new EnrollmentEligibilityPolicy(enrollments)
        .validateTransferSchedule(student.getId(), target.getId(), source.getId())).doesNotThrowAnyException();
  }

  @Test
  void anotherActiveEnrollmentStillBlocksTransfer() {
    enrollment(scheduledClass("OTHER"), EnrollmentStatus.CONFIRMED);
    entityManager.flush();
    assertThat(enrollments.existsScheduleConflictExcludingEnrollment(
        student.getId(), source.getId(), target.getId(), CAPACITY_RESERVED_STATUSES)).isTrue();
    assertThatThrownBy(() -> new EnrollmentEligibilityPolicy(enrollments)
        .validateTransferSchedule(student.getId(), target.getId(), source.getId())).hasMessageContaining("trùng thời gian");
  }

  @Test
  void cancelledEnrollmentDoesNotBlockTransfer() {
    enrollment(scheduledClass("CANCELLED"), EnrollmentStatus.CANCELLED);
    entityManager.flush();
    assertThat(enrollments.existsScheduleConflictExcludingEnrollment(
        student.getId(), source.getId(), target.getId(), CAPACITY_RESERVED_STATUSES)).isFalse();
  }

  @Test
  void duplicatePaymentNotificationIsStoredOnlyOnceAndKeepsReadState() {
    var userId = student.getUserId().getId();
    String key = "PAYMENT_SUCCESS:TX123:" + userId;
    notifications.insertPaymentNotification(userId, "Thanh toán", "Thành công", new Date(), key);
    notifications.markAllRead(userId, new Date());
    notifications.insertPaymentNotification(userId, "Thanh toán", "Thành công", new Date(), key);
    assertThat(notifications.count()).isEqualTo(1);
    assertThat(notifications.countByUserId_IdAndIsReadFalse(userId)).isZero();
  }

  @ParameterizedTest
  @EnumSource(value = EnrollmentPaymentStatus.class, names = {"CANCELLED", "REFUNDED"})
  void cancelledAttemptAllowsNewRegistrationAndPreservesHistory(EnrollmentPaymentStatus oldPayment) {
    source.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
    source.setPaymentStatus(oldPayment);
    source.setCancelledAt(new Date(1000));
    source.setCancellationReason("Previous cancellation");
    entityManager.flush();
    var service = registrationService();
    service.enrollMe(new CreateEnrollmentRequest(source.getCourseClassId().getId(), null), () -> "student@example.com");
    entityManager.flush();
    entityManager.clear();
    var attempts = enrollments.findByStudentId_IdOrderByEnrollmentDateDesc(student.getId());
    assertThat(attempts).hasSize(2);
    var previous = enrollments.findById(source.getId()).orElseThrow();
    assertThat(previous.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    assertThat(previous.getPaymentStatus()).isEqualTo(oldPayment);
    assertThat(previous.getCancellationReason()).isEqualTo("Previous cancellation");
    assertThat(previous.getCancelledAt().getTime()).isEqualTo(1000L);
    var current = attempts.stream().filter(e -> !e.getId().equals(source.getId())).findFirst().orElseThrow();
    assertThat(current.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    assertThat(current.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PENDING);
    assertThat(current.getCancelledAt()).isNull();
    assertThat(current.getCancellationReason()).isNull();
    assertThat(current.getPaymentDeadline().getTime() - current.getEnrollmentDate().getTime())
        .isEqualTo(java.time.Duration.ofDays(2).toMillis());
  }

  @Test
  void staffCanRegisterAfterRepeatedCancellations() {
    var service = registrationService();
    source.getCourseClassId().setStatus(ClassStatus.FULL);
    service.requestCancel(source.getId(), new CancelEnrollmentRequest("Changed plans"),
        () -> "student@example.com");
    assertThat(source.getCourseClassId().getStatus()).isEqualTo(ClassStatus.OPEN);
    enrollment(source.getCourseClassId(), EnrollmentStatus.CANCELLED);
    entityManager.flush();
    service.enrollByStaff(new StaffCreateEnrollmentRequest(
        source.getCourseClassId().getId(), "student@example.com"));
    assertThat(enrollments.findByStudentId_IdOrderByEnrollmentDateDesc(student.getId())).hasSize(3);
  }

  @ParameterizedTest
  @EnumSource(value = EnrollmentStatus.class, names = {"PENDING", "CONFIRMED"})
  void activeAttemptStillBlocksReRegistration(EnrollmentStatus status) {
    source.setEnrollmentStatus(status);
    entityManager.flush();
    assertThatThrownBy(() -> registrationService().enrollByStaff(new StaffCreateEnrollmentRequest(
        source.getCourseClassId().getId(), "student@example.com")))
        .isInstanceOf(com.ntt.language_center_management.exception.DuplicateResourceException.class);
    assertThat(enrollments.count()).isEqualTo(1);
  }

  @Test
  void databaseRejectsDuplicateActiveAttemptEvenWithoutPolicy() {
    assertThatThrownBy(() -> {
      enrollment(source.getCourseClassId(), EnrollmentStatus.PENDING);
      entityManager.flush();
    }).isInstanceOf(jakarta.persistence.PersistenceException.class);
  }

  @Test
  void transferCanReturnToClassWithCancelledAttempt() {
    enrollment(target, EnrollmentStatus.CANCELLED);
    entityManager.flush();
    registrationService().transfer(source.getId(), new TransferEnrollmentRequest(target.getId()));
    entityManager.flush();
    assertThat(enrollments.findById(source.getId()).orElseThrow().getCourseClassId().getId())
        .isEqualTo(target.getId());
    assertThat(enrollments.count()).isEqualTo(2);
  }

  private EnrollmentServiceImpl registrationService() {
    var clock = Clock.fixed(java.time.Instant.parse("2026-09-01T00:00:00Z"), java.time.ZoneOffset.UTC);
    var eligibility = new EnrollmentEligibilityPolicy(enrollments);
    var transfer = new EnrollmentTransferPolicy(clock);
    var users = mock(UserRepository.class);
    when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student.getUserId()));
    var resolver = new CurrentUserResolver(users, students, mock(TeacherRepository.class));
    return new EnrollmentServiceImpl(enrollments, classes, students, users,
        mock(EnrollmentMapper.class), mock(CourseMapper.class), mock(CourseClassMapper.class),
        mock(ClassScheduleMapper.class), mock(ClassScheduleRepository.class), resolver,
        new EnrollmentAccessPolicy(), eligibility, new EnrollmentCancellationPolicy(clock), transfer,
        new EnrollmentFactory(clock), new EnrollmentLifecycle(clock),
        new EnrollmentValidationPipelines(eligibility, transfer));
  }

  private Courseclass scheduledClass(String code) {
    var value = new Courseclass();
    value.setClassCode(code); value.setClassName(code); value.setCourseId(course);
    value.setStatus(ClassStatus.OPEN); value.setMaxStudents(20); value.setAppliedTuitionFee(BigDecimal.TEN);
    value.setStartDate(java.sql.Date.valueOf("2026-10-01")); value.setEndDate(java.sql.Date.valueOf("2026-12-31"));
    value.setCreatedAt(new Date()); persist(value);
    var schedule = new Classschedule(null, (short) 2, Time.valueOf("18:00:00"), Time.valueOf("20:00:00"), DeliveryMode.ONLINE);
    schedule.setCourseClassId(value); persist(schedule);
    return value;
  }

  private Enrollment enrollment(Courseclass courseClass, EnrollmentStatus status) {
    var enrollment = new EnrollmentFactory(Clock.systemUTC()).createConfirmed(student, courseClass);
    enrollment.setEnrollmentStatus(status);
    return persist(enrollment);
  }

  private <T> T persist(T value) { entityManager.persist(value); return value; }
}
