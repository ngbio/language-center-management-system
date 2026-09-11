package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ntt.language_center_management.dto.request.RefundRequest;
import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Refund;
import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.enums.PaymentTransactionStatus;
import com.ntt.language_center_management.enums.RefundStatus;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.repository.RefundRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.TeacherRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.transaction.TransactionExecutor;
import com.ntt.language_center_management.service.impl.BillingServiceImpl;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BillingServiceImplTest {
  private EnrollmentRepository enrollments;
  private PaymentRepository payments;
  private RefundRepository refunds;
  private UserRepository users;
  private BillingServiceImpl service;
  private CurrentUserResolver currentUserResolver;
  private TransactionExecutor transactionExecutor;
  private User owner;
  private Enrollment enrollment;

  @BeforeEach
  void setUp() {
    enrollments = mock(EnrollmentRepository.class);
    payments = mock(PaymentRepository.class);
    refunds = mock(RefundRepository.class);
    users = mock(UserRepository.class);
    currentUserResolver = new CurrentUserResolver(
        users, mock(StudentRepository.class), mock(TeacherRepository.class));
    transactionExecutor = new TransactionExecutor();
    service = new BillingServiceImpl(
        enrollments, payments, refunds, currentUserResolver, transactionExecutor);
    owner = user(70, "student@example.com", "STUDENT");
    enrollment = enrollment(owner);
    when(enrollments.findById(15)).thenReturn(Optional.of(enrollment));
    when(users.findByEmailIgnoreCase("student@example.com")).thenReturn(Optional.of(owner));
  }

  @Test
  void shouldAllowOwnerAndStaffToReadPayments() {
    Payment payment = payment("TX15", "3200000", PaymentTransactionStatus.PAID);
    when(payments.findByEnrollmentId_IdOrderByCreatedAtDesc(15)).thenReturn(List.of(payment));

    var ownerResult = service.getPayments(15, principal("student@example.com"));
    assertThat(ownerResult).singleElement().satisfies(value -> {
      assertThat(value.transactionCode()).isEqualTo("TX15");
      assertThat(value.status()).isEqualTo("PAID");
    });

    User admin = user(1, "admin@example.com", "ADMIN");
    when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
    assertThat(service.getPayments(15, principal("admin@example.com"))).hasSize(1);
  }

  @Test
  void shouldRejectAnotherStudentFromReadingFinancialData() {
    User outsider = user(71, "other@example.com", "STUDENT");
    when(users.findByEmailIgnoreCase("other@example.com")).thenReturn(Optional.of(outsider));

    assertThatThrownBy(() -> service.getPayments(15, principal("other@example.com")))
        .isInstanceOf(UnauthorizedException.class).hasMessageContaining("quyền");
    verify(payments, never()).findByEnrollmentId_IdOrderByCreatedAtDesc(15);
  }

  @Test
  void shouldRejectNonStaffAndInvalidRefundStatusFromStaffList() {
    assertThatThrownBy(() -> service.getStaffRefunds(null, principal("student@example.com")))
        .isInstanceOf(UnauthorizedException.class);

    User consultant = user(2, "staff@example.com", "CONSULTANT");
    when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(consultant));
    assertThatThrownBy(() -> service.getStaffRefunds("unknown", principal("staff@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không hợp lệ");
  }

  @Test
  void shouldReturnExistingRefundForSameIdempotencyKey() {
    User staff = user(2, "staff@example.com", "CONSULTANT");
    when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staff));
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
    Refund existing = refund(payment("TX15", "3200000", PaymentTransactionStatus.PAID),
        staff, "KEY-1", "100000", RefundStatus.PENDING);
    when(refunds.findByIdempotencyKey("KEY-1")).thenReturn(Optional.of(existing));

    var result = service.createRefund(15,
        new RefundRequest(new BigDecimal("100000"), "  Lý do  ", " KEY-1 "),
        principal("staff@example.com"));

    assertThat(result.amount()).isEqualByComparingTo("100000");
    assertThat(result.status()).isEqualTo("PENDING");
    verify(refunds, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldRejectRefundWithoutPaidPaymentOrBeyondRemainingAmount() {
    User staff = user(2, "staff@example.com", "CONSULTANT");
    when(users.findByEmailIgnoreCase("staff@example.com")).thenReturn(Optional.of(staff));
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
    when(refunds.findByIdempotencyKey("KEY-1")).thenReturn(Optional.empty());

    when(payments.findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
        15, PaymentTransactionStatus.PAID)).thenReturn(List.of());
    assertThatThrownBy(() -> service.createRefund(15,
        new RefundRequest(null, "Lý do", "KEY-1"), principal("staff@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("chưa có khoản");

    Payment paid = payment("TX15", "3200000", PaymentTransactionStatus.PAID);
    when(payments.findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
        15, PaymentTransactionStatus.PAID)).thenReturn(List.of(paid));
    when(refunds.findByEnrollment_IdOrderByCreatedAtDesc(15)).thenReturn(List.of());
    assertThatThrownBy(() -> service.createRefund(15,
        new RefundRequest(new BigDecimal("3200001"), "Lý do", "KEY-1"),
        principal("staff@example.com")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vượt quá");
  }

  @Test
  void shouldCalculateInvoicePaidRefundedAndNetAmounts() {
    Payment paid = payment("PAID", "3200000", PaymentTransactionStatus.PAID);
    paid.setCompletedAt(new Date(1_000));
    Payment failed = payment("FAILED", "3200000", PaymentTransactionStatus.FAILED);
    User staff = user(2, "staff@example.com", "CONSULTANT");
    Refund completed = refund(paid, staff, "KEY-1", "500000", RefundStatus.COMPLETED);
    Refund pending = refund(paid, staff, "KEY-2", "100000", RefundStatus.PENDING);
    when(payments.findByEnrollmentId_IdOrderByCreatedAtDesc(15)).thenReturn(List.of(paid, failed));
    when(refunds.findByEnrollment_IdOrderByCreatedAtDesc(15)).thenReturn(List.of(completed, pending));

    InvoiceResponse invoice = service.getInvoice(15, principal("student@example.com"));

    assertThat(invoice.invoiceNumber()).isEqualTo("INV-00000015");
    assertThat(invoice.paidAmount()).isEqualByComparingTo("3200000");
    assertThat(invoice.refundedAmount()).isEqualByComparingTo("500000");
    assertThat(invoice.netPaidAmount()).isEqualByComparingTo("2700000");
    assertThat(invoice.issuedAt()).isEqualTo(paid.getCompletedAt());
    assertThat(invoice.payments()).hasSize(2);
    assertThat(invoice.refunds()).hasSize(2);
  }

  @Test
  void shouldThrowNotFoundForMissingEnrollmentOrTransaction() {
    when(enrollments.findById(99)).thenReturn(Optional.empty());
    when(payments.findByTransactionCode("missing")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getPayments(99, principal("student@example.com")))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> service.getPayment("missing", principal("student@example.com")))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldCompleteMomoRefundAndRevokeLearningRightsAfterFullRefund() {
    User staff = configureStaff();
    Payment paid = payment("TX15", "3200000", PaymentTransactionStatus.PAID);
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new BillingServiceImpl(enrollments, payments, refunds, currentUserResolver,
        transactionExecutor, builder.build());
    configureMomoRefund();
    AtomicReference<Refund> created = prepareRefund(paid);
    server.expect(requestTo("https://momo.example.com/refund"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          assertThat(body).contains("\"amount\":3200000", "\"transId\":998877",
              "\"signature\":");
        })
        .andRespond(withSuccess("{\"resultCode\":0,\"transId\":\"RF-GATEWAY-1\"}",
            MediaType.APPLICATION_JSON));

    var result = service.createRefund(15,
        new RefundRequest(null, "Học viên yêu cầu", "KEY-MOMO"),
        principal(staff.getEmail()));

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(created.get().getGatewayRefundId()).isEqualTo("RF-GATEWAY-1");
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.REFUNDED);
    assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    verify(enrollments).save(enrollment);
    server.verify();
  }

  @Test
  void shouldMarkZaloPayRefundFailedWithoutChangingLearningRightsWhenGatewayRejects() {
    User staff = configureStaff();
    Payment paid = payment("TX15", "3200000", PaymentTransactionStatus.PAID);
    paid.setMethod(PaymentMethod.ZALOPAY);
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new BillingServiceImpl(enrollments, payments, refunds, currentUserResolver,
        transactionExecutor, builder.build());
    configureZaloRefund();
    AtomicReference<Refund> created = prepareRefund(paid);
    server.expect(requestTo("https://zalo.example.com/refund"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          assertThat(body).contains("app_id=2553", "zp_trans_id=998877", "amount=3200000", "mac=");
        })
        .andRespond(withSuccess("{\"return_code\":2,\"return_message\":\"rejected\"}",
            MediaType.APPLICATION_JSON));

    var result = service.createRefund(15,
        new RefundRequest(null, "Học viên yêu cầu", "KEY-ZALO"),
        principal(staff.getEmail()));

    assertThat(result.status()).isEqualTo("FAILED");
    assertThat(created.get().getErrorMessage()).isEqualTo("rejected");
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PAID);
    assertThat(enrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
    verify(enrollments, never()).save(enrollment);
    server.verify();
  }

  @Test
  void shouldKeepRefundPendingWhenGatewayConnectionTimesOut() {
    User staff = configureStaff();
    Payment paid = payment("TX15", "3200000", PaymentTransactionStatus.PAID);
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new BillingServiceImpl(enrollments, payments, refunds, currentUserResolver,
        transactionExecutor, builder.build());
    configureMomoRefund();
    AtomicReference<Refund> created = prepareRefund(paid);
    server.expect(requestTo("https://momo.example.com/refund"))
        .andRespond(request -> { throw new java.io.IOException("connection timeout"); });

    var result = service.createRefund(15,
        new RefundRequest(new BigDecimal("100000"), "Đối soát", "KEY-TIMEOUT"),
        principal(staff.getEmail()));

    assertThat(result.status()).isEqualTo("PENDING");
    assertThat(created.get().getErrorMessage()).contains("timeout");
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PAID);
  }

  @Test
  void shouldReturnFinishedRefundWithoutCallingGatewayOnRefresh() {
    User staff = configureStaff();
    Refund completed = refund(payment("TX15", "3200000", PaymentTransactionStatus.PAID),
        staff, "KEY-1", "100000", RefundStatus.COMPLETED);
    when(refunds.findById(5)).thenReturn(Optional.of(completed));

    var result = service.refreshRefund(5, principal(staff.getEmail()));

    assertThat(result.status()).isEqualTo("COMPLETED");
    verify(refunds, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldRefreshPendingMomoRefundAndCompleteItFromGatewayQuery() {
    User staff = configureStaff();
    Payment paid = payment("TX15", "3200000", PaymentTransactionStatus.PAID);
    Refund pending = refund(paid, staff, "KEY-1", "100000", RefundStatus.PENDING);
    when(refunds.findById(5)).thenReturn(Optional.of(pending));
    when(payments.findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
        15, PaymentTransactionStatus.PAID)).thenReturn(List.of(paid));
    when(refunds.findByEnrollment_IdOrderByCreatedAtDesc(15)).thenReturn(List.of(pending));
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new BillingServiceImpl(enrollments, payments, refunds, currentUserResolver,
        transactionExecutor, builder.build());
    configureMomoRefund();
    ReflectionTestUtils.setField(service, "momoRefundQueryEndpoint", "https://momo.example.com/query");
    server.expect(requestTo("https://momo.example.com/query"))
        .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString())
            .contains("\"orderId\":\"RF-KEY-1\"", "\"signature\":"))
        .andRespond(withSuccess("{\"resultCode\":0,\"message\":\"successful\"}",
            MediaType.APPLICATION_JSON));

    var result = service.refreshRefund(5, principal(staff.getEmail()));

    assertThat(result.status()).isEqualTo("COMPLETED");
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PAID);
    server.verify();
  }

  @Test
  void shouldRefreshPendingZaloPayRefundUsingZaloPayQueryEndpoint() {
    User staff = configureStaff();
    Payment paid = payment("TX15", "3200000", PaymentTransactionStatus.PAID);
    paid.setMethod(PaymentMethod.ZALOPAY);
    Refund pending = refund(paid, staff, "KEY-2", "100000", RefundStatus.PENDING);
    when(refunds.findById(6)).thenReturn(Optional.of(pending));
    when(payments.findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
        15, PaymentTransactionStatus.PAID)).thenReturn(List.of(paid));
    when(refunds.findByEnrollment_IdOrderByCreatedAtDesc(15)).thenReturn(List.of(pending));
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new BillingServiceImpl(enrollments, payments, refunds, currentUserResolver,
        transactionExecutor, builder.build());
    configureZaloRefund();
    ReflectionTestUtils.setField(service, "zaloPayRefundQueryEndpoint", "https://zalo.example.com/query");
    server.expect(requestTo("https://zalo.example.com/query"))
        .andExpect(request -> assertThat(((MockClientHttpRequest) request).getBodyAsString())
            .contains("app_id=2553", "m_refund_id=RF-KEY-2", "mac="))
        .andRespond(withSuccess("{\"return_code\":1,\"return_message\":\"success\"}",
            MediaType.APPLICATION_JSON));

    var result = service.refreshRefund(6, principal(staff.getEmail()));

    assertThat(result.status()).isEqualTo("COMPLETED");
    server.verify();
  }

  @Test
  void shouldRejectReusedIdempotencyKeyForDifferentRefundRequest() {
    User staff = configureStaff();
    Refund existing = refund(payment("TX15", "3200000", PaymentTransactionStatus.PAID),
        staff, "SAME-KEY", "100000", RefundStatus.PENDING);
    when(refunds.findByIdempotencyKey("SAME-KEY")).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> service.createRefund(15,
        new RefundRequest(new BigDecimal("200000"), "Khác", "SAME-KEY"),
        principal(staff.getEmail())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Idempotency key");
  }

  private Principal principal(String email) {
    return () -> email;
  }

  private User configureStaff() {
    User staff = user(2, "staff@example.com", "CONSULTANT");
    when(users.findByEmailIgnoreCase(staff.getEmail())).thenReturn(Optional.of(staff));
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
    when(refunds.findByIdempotencyKey(org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(Optional.empty());
    return staff;
  }

  private AtomicReference<Refund> prepareRefund(Payment paid) {
    AtomicReference<Refund> created = new AtomicReference<>();
    when(payments.findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
        15, PaymentTransactionStatus.PAID)).thenReturn(List.of(paid));
    when(refunds.existsByEnrollment_IdAndStatus(15, RefundStatus.PENDING)).thenReturn(false);
    when(refunds.sumAmountByEnrollmentIdAndStatus(15, RefundStatus.COMPLETED))
        .thenAnswer(invocation -> created.get() != null
                && created.get().getStatus() == RefundStatus.COMPLETED
            ? created.get().getAmount()
            : BigDecimal.ZERO);
    when(refunds.saveAndFlush(org.mockito.ArgumentMatchers.any(Refund.class)))
        .thenAnswer(invocation -> {
          Refund value = invocation.getArgument(0);
          created.set(value);
          return value;
        });
    when(refunds.save(org.mockito.ArgumentMatchers.any(Refund.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    return created;
  }

  private void configureMomoRefund() {
    ReflectionTestUtils.setField(service, "momoRefundEndpoint", "https://momo.example.com/refund");
    ReflectionTestUtils.setField(service, "momoPartnerCode", "PARTNER");
    ReflectionTestUtils.setField(service, "momoAccessKey", "ACCESS");
    ReflectionTestUtils.setField(service, "momoSecretKey", "SECRET");
  }

  private void configureZaloRefund() {
    ReflectionTestUtils.setField(service, "zaloPayRefundEndpoint", "https://zalo.example.com/refund");
    ReflectionTestUtils.setField(service, "zaloPayAppId", "2553");
    ReflectionTestUtils.setField(service, "zaloPayKey1", "ZALO-KEY-1");
  }

  private User user(int id, String email, String roleCode) {
    User user = new User(id);
    user.setEmail(email);
    user.setFullName("User " + id);
    user.setRoleId(new Role(id, roleCode, roleCode));
    return user;
  }

  private Enrollment enrollment(User studentUser) {
    Student student = new Student(7);
    student.setStudentCode("HV000007");
    student.setUserId(studentUser);
    Course course = new Course(3);
    course.setCourseCode("EN-A1");
    course.setCourseName("English A1");
    Courseclass courseClass = new Courseclass(4);
    courseClass.setCourseId(course);
    courseClass.setClassCode("EN-A1-01");
    courseClass.setClassName("English A1 Evening");
    Enrollment value = new Enrollment(15);
    value.setStudentId(student);
    value.setCourseClassId(courseClass);
    value.setEnrollmentDate(new Date(500));
    value.setAmountDue(new BigDecimal("3200000"));
    value.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    value.setPaymentStatus(EnrollmentPaymentStatus.PAID);
    return value;
  }

  private Payment payment(String code, String amount, PaymentTransactionStatus status) {
    Payment payment = new Payment(5);
    payment.setEnrollmentId(enrollment);
    payment.setTransactionCode(code);
    payment.setMethod(PaymentMethod.MOMO);
    payment.setAmount(new BigDecimal(amount));
    payment.setStatus(status);
    payment.setCreatedAt(new Date());
    payment.setReferenceCode("998877");
    return payment;
  }

  private Refund refund(Payment payment, User staff, String key, String amount, RefundStatus status) {
    Refund refund = new Refund();
    refund.setEnrollment(enrollment);
    refund.setPayment(payment);
    refund.setProcessedBy(staff);
    refund.setRefundCode("RF-" + key);
    refund.setIdempotencyKey(key);
    refund.setAmount(new BigDecimal(amount));
    refund.setReason("Lý do");
    refund.setStatus(status);
    refund.setCreatedAt(new Date());
    return refund;
  }
}
