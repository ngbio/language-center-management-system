package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.RefundRequest;
import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.dto.response.RefundResponse;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Refund;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.ClassStatus;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.PaymentTransactionStatus;
import com.ntt.language_center_management.enums.RefundStatus;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.payment.RefundCommand;
import com.ntt.language_center_management.payment.RefundGateway;
import com.ntt.language_center_management.payment.RefundGatewayRegistry;
import com.ntt.language_center_management.payment.RefundResult;
import com.ntt.language_center_management.repository.CourseClassRepository;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.repository.RefundRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.BillingService;
import com.ntt.language_center_management.service.EnrollmentLifecycle;
import com.ntt.language_center_management.transaction.TransactionExecutor;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import static com.ntt.language_center_management.policy.EnrollmentPolicy.CAPACITY_RESERVED_STATUSES;

@Service
public class BillingServiceImpl implements BillingService {
  private final EnrollmentRepository enrollmentRepository;
  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final CurrentUserResolver currentUserResolver;
  private final TransactionExecutor transactionExecutor;
  private final RefundGatewayRegistry gateways;
  private final EnrollmentLifecycle lifecycle;
  private final CourseClassRepository courseClassRepository;

  public BillingServiceImpl(EnrollmentRepository enrollmentRepository, PaymentRepository paymentRepository,
      RefundRepository refundRepository, CurrentUserResolver currentUserResolver,
      TransactionExecutor transactionExecutor, RefundGatewayRegistry gateways, EnrollmentLifecycle lifecycle,
      CourseClassRepository courseClassRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.paymentRepository = paymentRepository;
    this.refundRepository = refundRepository;
    this.currentUserResolver = currentUserResolver;
    this.transactionExecutor = transactionExecutor;
    this.gateways = gateways;
    this.lifecycle = lifecycle;
    this.courseClassRepository = courseClassRepository;
  }

  @Override @Transactional(readOnly = true)
  public List<PaymentResponse> getPayments(Integer enrollmentId, Principal principal) {
    Enrollment enrollment = requireEnrollment(enrollmentId);
    requireOwnerOrStaff(enrollment, currentUser(principal));
    return paymentRepository.findByEnrollmentId_IdOrderByCreatedAtDesc(enrollmentId).stream()
        .map(this::paymentResponse).toList();
  }

  @Override @Transactional(readOnly = true)
  public PaymentResponse getPayment(String transactionCode, Principal principal) {
    Payment payment = paymentRepository.findByTransactionCode(transactionCode)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
    requireOwnerOrStaff(payment.getEnrollmentId(), currentUser(principal));
    return paymentResponse(payment);
  }

  @Override @Transactional(readOnly = true)
  public List<RefundResponse> getRefunds(Integer enrollmentId, Principal principal) {
    Enrollment enrollment = requireEnrollment(enrollmentId);
    requireOwnerOrStaff(enrollment, currentUser(principal));
    return refundRepository.findByEnrollment_IdOrderByCreatedAtDesc(enrollmentId).stream()
        .map(this::refundResponse).toList();
  }

  @Override @Transactional(readOnly = true)
  public List<RefundResponse> getStaffRefunds(String status, Principal principal) {
    requireStaff(currentUser(principal));
    List<Refund> refunds;
    if (StringUtils.hasText(status)) {
      String normalized = status.trim().toUpperCase();
      if (!List.of("PENDING", "COMPLETED", "FAILED", "CANCELLED").contains(normalized)) {
        throw new IllegalArgumentException("Trạng thái hoàn tiền không hợp lệ");
      }
      refunds = refundRepository.findByStatusOrderByCreatedAtDesc(RefundStatus.valueOf(normalized));
    } else {
      refunds = refundRepository.findAllByOrderByCreatedAtDesc();
    }
    return refunds.stream().map(this::refundResponse).toList();
  }

  @Override
  public RefundResponse createRefund(Integer enrollmentId, RefundRequest request, Principal principal) {
    RefundPreparation preparation = transactionExecutor.required(
        () -> prepareRefund(enrollmentId, request, principal));
    Refund refund = preparation.refund();
    if (!preparation.shouldSubmit()) return refundResponse(refund);
    Payment payment = refund.getPayment();
    try {
      applyRefundResult(refund, gateways.getRequired(payment.getMethod()).submit(refundCommand(refund)));
    } catch (RuntimeException exception) {
      // Timeout/mất kết nối không chứng minh gateway đã từ chối. Giữ PENDING để đối soát.
      refund.setErrorMessage(gatewayMessage(exception));
      updateState(() -> refundRepository.save(refund));
    }
    return refundResponse(refund);
  }

  private RefundPreparation prepareRefund(
      Integer enrollmentId, RefundRequest request, Principal principal) {
    User actor = currentUser(principal);
    requireStaff(actor);
    Enrollment enrollment = enrollmentRepository.lockById(enrollmentId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
    Refund previous = refundRepository.findByIdempotencyKey(request.idempotencyKey().trim()).orElse(null);
    if (previous != null) {
      if (!previous.getEnrollment().getId().equals(enrollmentId)
          || (request.amount() != null && previous.getAmount().compareTo(request.amount()) != 0)) {
        throw new IllegalArgumentException("Idempotency key đã được dùng cho yêu cầu hoàn tiền khác");
      }
      return new RefundPreparation(previous, false);
    }
    List<Payment> paidPayments = paymentRepository
        .findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
            enrollmentId, PaymentTransactionStatus.PAID);
    if (paidPayments.isEmpty() || enrollment.getPaymentStatus() != EnrollmentPaymentStatus.PAID) {
      throw new IllegalArgumentException("Đăng ký chưa có khoản thanh toán thành công để hoàn");
    }
    if (refundRepository.existsByEnrollment_IdAndStatus(enrollmentId, RefundStatus.PENDING)) {
      throw new IllegalArgumentException("Đăng ký đang có một yêu cầu hoàn tiền chờ xử lý");
    }
    BigDecimal paid = paidPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunded = completedRefundTotal(enrollmentId);
    BigDecimal refundable = paid.subtract(refunded);
    BigDecimal amount = request.amount() == null ? refundable : request.amount();
    if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(refundable) > 0) {
      throw new IllegalArgumentException("Số tiền hoàn vượt quá số tiền thực thu còn lại: " + refundable);
    }

    Payment payment = paidPayments.get(0);
    RefundGateway gateway = gateways.getRequired(payment.getMethod());
    gateway.validate(payment.getReferenceCode());
    Date now = new Date();
    Refund refund = new Refund();
    refund.setEnrollment(enrollment);
    refund.setPayment(payment);
    refund.setProcessedBy(actor);
    refund.setRefundCode(gateway.createRefundCode(enrollmentId));
    refund.setIdempotencyKey(request.idempotencyKey().trim());
    refund.setAmount(amount);
    refund.setReason(request.reason().trim());
    refund.setStatus(RefundStatus.PENDING);
    refund.setCreatedAt(now);
    return new RefundPreparation(refundRepository.saveAndFlush(refund), true);
  }

  @Override
  public RefundResponse refreshRefund(Integer refundId, Principal principal) {
    requireStaff(currentUser(principal));
    Refund refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));
    if (refund.getStatus() != RefundStatus.PENDING) return refundResponse(refund);
    try {
      applyRefundResult(refund, gateways.getRequired(refund.getPayment().getMethod()).query(refundCommand(refund)));
    } catch (RuntimeException exception) {
      refund.setErrorMessage(gatewayMessage(exception));
      updateState(() -> refundRepository.save(refund));
    }
    return refundResponse(refund);
  }

  private RefundCommand refundCommand(Refund refund) {
    return new RefundCommand(refund.getRefundCode(), refund.getIdempotencyKey(),
        refund.getPayment().getReferenceCode(), refund.getAmount(), refund.getReason());
  }

  private void applyRefundResult(Refund refund, RefundResult result) {
    updateState(() -> {
      if (result.gatewayRefundId() != null) refund.setGatewayRefundId(result.gatewayRefundId());
      switch (result.status()) {
        case COMPLETED -> completeRefund(refund);
        case FAILED -> failRefund(refund, result.message());
        case PENDING -> {
          refund.setErrorMessage(result.message());
          refundRepository.save(refund);
        }
        default -> throw new IllegalArgumentException("Trạng thái hoàn tiền từ gateway không hợp lệ");
      }
    });
  }

  private void completeRefund(Refund refund) {
    if (refund.getStatus() == RefundStatus.COMPLETED) return;
    Date now = new Date();
    refund.setStatus(RefundStatus.COMPLETED);
    refund.setErrorMessage(null);
    refund.setCompletedAt(now);
    refundRepository.save(refund);
    BigDecimal paid = paymentRepository.findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
        refund.getEnrollment().getId(), PaymentTransactionStatus.PAID).stream().map(Payment::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    if (completedRefundTotal(refund.getEnrollment().getId()).compareTo(paid) >= 0) {
      Enrollment enrollment = enrollmentRepository.lockById(refund.getEnrollment().getId())
          .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
      var courseClass = courseClassRepository.lockById(enrollment.getCourseClassId().getId())
          .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lớp học"));
      lifecycle.markFullyRefunded(enrollment, refund.getReason(), now);
      enrollmentRepository.saveAndFlush(enrollment);
      if (courseClass.getStatus() == ClassStatus.FULL
          && enrollmentRepository.countByCourseClassId_IdAndEnrollmentStatusIn(
              courseClass.getId(), CAPACITY_RESERVED_STATUSES) < courseClass.getMaxStudents()) {
        courseClass.setStatus(ClassStatus.OPEN);
        courseClassRepository.save(courseClass);
      }
    }
  }

  private void failRefund(Refund refund, String message) {
    refund.setStatus(RefundStatus.FAILED);
    refund.setErrorMessage(message);
    refundRepository.save(refund);
  }

  private void updateState(Runnable action) {
    transactionExecutor.required(() -> {
      action.run();
      return null;
    });
  }

  private record RefundPreparation(Refund refund, boolean shouldSubmit) {}

  @Override @Transactional(readOnly = true)
  public InvoiceResponse getInvoice(Integer enrollmentId, Principal principal) {
    Enrollment enrollment = requireEnrollment(enrollmentId);
    requireOwnerOrStaff(enrollment, currentUser(principal));
    List<Payment> payments = paymentRepository.findByEnrollmentId_IdOrderByCreatedAtDesc(enrollmentId);
    List<Refund> refunds = refundRepository.findByEnrollment_IdOrderByCreatedAtDesc(enrollmentId);
    BigDecimal paid = payments.stream().filter(value -> value.getStatus() == PaymentTransactionStatus.PAID)
        .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunded = refunds.stream().filter(value -> value.getStatus() == RefundStatus.COMPLETED)
        .map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var student = enrollment.getStudentId();
    var courseClass = enrollment.getCourseClassId();
    var course = courseClass.getCourseId();
    Date issuedAt = payments.stream().filter(value -> value.getStatus() == PaymentTransactionStatus.PAID)
        .map(Payment::getCompletedAt).filter(value -> value != null).findFirst().orElse(enrollment.getEnrollmentDate());
    return new InvoiceResponse("INV-" + String.format("%08d", enrollmentId), enrollmentId,
        student.getStudentCode(), student.getUserId().getFullName(), student.getUserId().getEmail(),
        course.getCourseCode(), course.getCourseName(), courseClass.getClassCode(), courseClass.getClassName(),
        enrollment.getAmountDue(), paid, refunded, paid.subtract(refunded), enrollment.getEnrollmentStatus().name(),
        enrollment.getPaymentStatus().name(), issuedAt, payments.stream().map(this::paymentResponse).toList(),
        refunds.stream().map(this::refundResponse).toList());
  }

  private BigDecimal completedRefundTotal(Integer enrollmentId) {
    BigDecimal total = refundRepository.sumAmountByEnrollmentIdAndStatus(
        enrollmentId, RefundStatus.COMPLETED);
    return total == null ? BigDecimal.ZERO : total;
  }
  private Enrollment requireEnrollment(Integer id) { return enrollmentRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký")); }
  private User currentUser(Principal principal) {
    return currentUserResolver.requireUser(principal);
  }
  private void requireOwnerOrStaff(Enrollment enrollment, User user) {
    if (enrollment.getStudentId().getUserId().getId().equals(user.getId())) return;
    requireStaff(user);
  }
  private void requireStaff(User user) {
    String role = user.getRoleId() == null ? "" : user.getRoleId().getRoleCode();
    if (!List.of("ADMIN", "CONSULTANT").contains(role)) throw new UnauthorizedException("Không có quyền xử lý tài chính");
  }
  private PaymentResponse paymentResponse(Payment payment) { return new PaymentResponse(payment.getId(),
      payment.getEnrollmentId().getId(), payment.getTransactionCode(), payment.getMethod().name(), payment.getAmount(),
      payment.getStatus().name(), null, payment.getCreatedAt(), payment.getCompletedAt()); }
  private RefundResponse refundResponse(Refund refund) { return new RefundResponse(refund.getId(),
      refund.getEnrollment().getId(), refund.getPayment().getId(), refund.getRefundCode(),
      refund.getPayment().getMethod().name(), refund.getEnrollment().getStudentId().getUserId().getFullName(),
      refund.getEnrollment().getCourseClassId().getClassName(), refund.getAmount(),
      refund.getStatus().name(), refund.getGatewayRefundId(), refund.getErrorMessage(), refund.getReason(),
      refund.getProcessedBy().getId(), refund.getProcessedBy().getFullName(),
      refund.getCreatedAt(), refund.getCompletedAt()); }

  private String gatewayMessage(RuntimeException exception) {
    return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : "Không thể kết nối cổng thanh toán";
  }
}
