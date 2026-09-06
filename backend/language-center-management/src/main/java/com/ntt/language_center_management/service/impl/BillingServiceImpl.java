package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.RefundRequest;
import com.ntt.language_center_management.dto.response.InvoiceResponse;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.dto.response.RefundResponse;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Refund;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.repository.RefundRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.BillingService;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class BillingServiceImpl implements BillingService {
  private final EnrollmentRepository enrollmentRepository;
  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final UserRepository userRepository;

  public BillingServiceImpl(EnrollmentRepository enrollmentRepository, PaymentRepository paymentRepository,
      RefundRepository refundRepository, UserRepository userRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.paymentRepository = paymentRepository;
    this.refundRepository = refundRepository;
    this.userRepository = userRepository;
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

  @Override
  public RefundResponse createRefund(Integer enrollmentId, RefundRequest request, Principal principal) {
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
      return refundResponse(previous);
    }
    List<Payment> paidPayments = paymentRepository
        .findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(enrollmentId, "PAID");
    if (paidPayments.isEmpty() || !"PAID".equals(enrollment.getPaymentStatus())) {
      throw new IllegalArgumentException("Đăng ký chưa có khoản thanh toán thành công để hoàn");
    }
    BigDecimal paid = paidPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunded = completedRefundTotal(enrollmentId);
    BigDecimal refundable = paid.subtract(refunded);
    BigDecimal amount = request.amount() == null ? refundable : request.amount();
    if (amount.compareTo(BigDecimal.ZERO) <= 0 || amount.compareTo(refundable) > 0) {
      throw new IllegalArgumentException("Số tiền hoàn vượt quá số tiền thực thu còn lại: " + refundable);
    }

    Date now = new Date();
    Refund refund = new Refund();
    refund.setEnrollment(enrollment);
    refund.setPayment(paidPayments.get(0));
    refund.setProcessedBy(actor);
    refund.setRefundCode("RF" + enrollmentId + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
    refund.setIdempotencyKey(request.idempotencyKey().trim());
    refund.setAmount(amount);
    refund.setReason(request.reason().trim());
    refund.setStatus("COMPLETED");
    refund.setCreatedAt(now);
    refund.setCompletedAt(now);
    refund = refundRepository.save(refund);

    if (amount.compareTo(refundable) == 0) {
      enrollment.setPaymentStatus("REFUNDED");
      enrollment.setEnrollmentStatus("CANCELLED");
      enrollment.setCancelledAt(now);
      enrollment.setCancellationReason(request.reason().trim());
      enrollmentRepository.save(enrollment);
    }
    return refundResponse(refund);
  }

  @Override @Transactional(readOnly = true)
  public InvoiceResponse getInvoice(Integer enrollmentId, Principal principal) {
    Enrollment enrollment = requireEnrollment(enrollmentId);
    requireOwnerOrStaff(enrollment, currentUser(principal));
    List<Payment> payments = paymentRepository.findByEnrollmentId_IdOrderByCreatedAtDesc(enrollmentId);
    List<Refund> refunds = refundRepository.findByEnrollment_IdOrderByCreatedAtDesc(enrollmentId);
    BigDecimal paid = payments.stream().filter(value -> "PAID".equals(value.getStatus()))
        .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal refunded = refunds.stream().filter(value -> "COMPLETED".equals(value.getStatus()))
        .map(Refund::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var student = enrollment.getStudentId();
    var courseClass = enrollment.getCourseClassId();
    var course = courseClass.getCourseId();
    Date issuedAt = payments.stream().filter(value -> "PAID".equals(value.getStatus()))
        .map(Payment::getCompletedAt).filter(value -> value != null).findFirst().orElse(enrollment.getEnrollmentDate());
    return new InvoiceResponse("INV-" + String.format("%08d", enrollmentId), enrollmentId,
        student.getStudentCode(), student.getUserId().getFullName(), student.getUserId().getEmail(),
        course.getCourseCode(), course.getCourseName(), courseClass.getClassCode(), courseClass.getClassName(),
        enrollment.getAmountDue(), paid, refunded, paid.subtract(refunded), enrollment.getEnrollmentStatus(),
        enrollment.getPaymentStatus(), issuedAt, payments.stream().map(this::paymentResponse).toList(),
        refunds.stream().map(this::refundResponse).toList());
  }

  private BigDecimal completedRefundTotal(Integer enrollmentId) {
    return refundRepository.findByEnrollment_IdOrderByCreatedAtDesc(enrollmentId).stream()
        .filter(value -> "COMPLETED".equals(value.getStatus())).map(Refund::getAmount)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }
  private Enrollment requireEnrollment(Integer id) { return enrollmentRepository.findById(id)
      .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký")); }
  private User currentUser(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) throw new UnauthorizedException("Chưa đăng nhập");
    return userRepository.findByEmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
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
      payment.getEnrollmentId().getId(), payment.getTransactionCode(), payment.getMethod(), payment.getAmount(),
      payment.getStatus(), null, payment.getCreatedAt(), payment.getCompletedAt()); }
  private RefundResponse refundResponse(Refund refund) { return new RefundResponse(refund.getId(),
      refund.getEnrollment().getId(), refund.getPayment().getId(), refund.getRefundCode(), refund.getAmount(),
      refund.getStatus(), refund.getReason(), refund.getProcessedBy().getId(), refund.getProcessedBy().getFullName(),
      refund.getCreatedAt(), refund.getCompletedAt()); }
}
