package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.CreatePaymentRequest;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.enums.PaymentTransactionStatus;
import com.ntt.language_center_management.event.PaymentSucceededEvent;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.payment.PaymentCallback;
import com.ntt.language_center_management.payment.PaymentCheckout;
import com.ntt.language_center_management.payment.PaymentCommand;
import com.ntt.language_center_management.payment.PaymentGatewayRegistry;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.EnrollmentExpirationService;
import com.ntt.language_center_management.service.EnrollmentLifecycle;
import com.ntt.language_center_management.service.PaymentService;
import com.ntt.language_center_management.transaction.TransactionExecutor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CurrentUserResolver currentUserResolver;
  private final EnrollmentExpirationService enrollmentExpirationService;
  private final TransactionExecutor transactionExecutor;
  private final ApplicationEventPublisher eventPublisher;
  private final PaymentGatewayRegistry gateways;
  private final EnrollmentLifecycle lifecycle;

  public PaymentServiceImpl(PaymentRepository paymentRepository,
      EnrollmentRepository enrollmentRepository, CurrentUserResolver currentUserResolver,
      EnrollmentExpirationService enrollmentExpirationService, TransactionExecutor transactionExecutor,
      ApplicationEventPublisher eventPublisher, PaymentGatewayRegistry gateways, EnrollmentLifecycle lifecycle) {
    this.paymentRepository = paymentRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.currentUserResolver = currentUserResolver;
    this.enrollmentExpirationService = enrollmentExpirationService;
    this.transactionExecutor = transactionExecutor;
    this.eventPublisher = eventPublisher;
    this.gateways = gateways;
    this.lifecycle = lifecycle;
  }

  @Override
  public PaymentResponse createPayment(CreatePaymentRequest request, Principal principal) {
    PaymentPreparation preparation = transactionExecutor.required(
        () -> preparePayment(request, principal));
    PaymentCheckout checkout = gateways.getRequired(preparation.method())
        .createPayment(preparation.command());
    Payment payment = savePending(preparation.enrollment(), checkout.transactionCode(), preparation.method());
    return toResponse(payment, checkout.paymentUrl());
  }

  private PaymentPreparation preparePayment(CreatePaymentRequest request, Principal principal) {
    Student student = currentStudent(principal);
    if (enrollmentExpirationService.expireIfOverdue(request.enrollmentId())) {
      throw new IllegalArgumentException("Đăng ký đã hết hạn thanh toán 48 giờ");
    }
    Enrollment enrollment = enrollmentRepository.lockById(request.enrollmentId())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
    if (!enrollment.getStudentId().getId().equals(student.getId())) {
      throw new UnauthorizedException("Bạn không được thanh toán đăng ký của học viên khác");
    }
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.CANCELLED) {
      throw new IllegalArgumentException("Đăng ký đã bị hủy");
    }
    if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED) {
      throw new IllegalArgumentException("Đăng ký không ở trạng thái được phép thanh toán");
    }
    if (enrollment.getPaymentStatus() == EnrollmentPaymentStatus.PAID) {
      throw new IllegalArgumentException("Đăng ký đã được thanh toán");
    }
    if (enrollment.getAmountDue() == null || enrollment.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Đăng ký miễn phí không cần tạo giao dịch thanh toán");
    }

    return new PaymentPreparation(enrollment,
        new PaymentCommand(enrollment.getId(), student.getId(),
            enrollment.getCourseClassId().getClassCode(), amount(enrollment)), request.method());
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentResponse> getMyPayments(Principal principal) {
    Student student = currentStudent(principal);
    return paymentRepository.findByEnrollmentId_StudentId_IdOrderByCreatedAtDesc(student.getId())
        .stream().map(payment -> toResponse(payment, null)).toList();
  }

  @Override
  @Transactional
  public Map<String, Object> handleMomoIpn(Map<String, Object> payload) {
    return gateways.getRequired(PaymentMethod.MOMO).handleCallback(payload,
        callback -> applyCallback(PaymentMethod.MOMO, callback));
  }

  @Override
  @Transactional
  public Map<String, Object> handleZaloPayCallback(Map<String, Object> payload) {
    return gateways.getRequired(PaymentMethod.ZALOPAY).handleCallback(payload,
        callback -> applyCallback(PaymentMethod.ZALOPAY, callback));
  }

  private void applyCallback(PaymentMethod method, PaymentCallback callback) {
    Payment payment = findPayment(callback.transactionCode(), method);
    if (callback.succeeded()) complete(payment, Long.parseLong(callback.amount()), callback.referenceCode());
    else fail(payment, callback.errorMessage());
  }

  private Payment savePending(Enrollment enrollment, String code, PaymentMethod method) {
    Payment payment = new Payment();
    payment.setEnrollmentId(enrollment);
    payment.setTransactionCode(code);
    payment.setMethod(method);
    payment.setAmount(enrollment.getAmountDue());
    payment.setStatus(PaymentTransactionStatus.PENDING);
    payment.setCreatedAt(new Date());
    return paymentRepository.save(payment);
  }

  private void complete(Payment payment, long paidAmount, String reference) {
    if (payment.getStatus() == PaymentTransactionStatus.PAID) return;
    if (paidAmount != amount(payment.getEnrollmentId())) throw new IllegalArgumentException("Số tiền thanh toán không khớp");
    Enrollment enrollment = payment.getEnrollmentId();
    Date now = new Date();
    lifecycle.markPaid(enrollment, now);
    payment.setStatus(PaymentTransactionStatus.PAID);
    payment.setCompletedAt(now);
    payment.setReferenceCode(reference);
    paymentRepository.save(payment);
    enrollmentRepository.save(enrollment);
    User paymentUser = enrollment.getStudentId().getUserId();
    eventPublisher.publishEvent(new PaymentSucceededEvent(
        paymentUser.getId(), paymentUser.getEmail(), paymentUser.getFullName(), payment.getTransactionCode(),
        enrollment.getCourseClassId().getClassName(), payment.getAmount()));
  }

  private void fail(Payment payment, String message) {
    if (payment.getStatus() != PaymentTransactionStatus.PAID) {
      payment.setStatus(PaymentTransactionStatus.FAILED);
      payment.setErrorMessage(message);
      paymentRepository.save(payment);
    }
  }

  private Payment findPayment(String code, PaymentMethod method) {
    return paymentRepository.findByTransactionCodeAndMethod(code, method)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
  }

  private Student currentStudent(Principal principal) {
    return currentUserResolver.requireStudent(principal);
  }

  private record PaymentPreparation(
      Enrollment enrollment, PaymentCommand command, PaymentMethod method) {}

  private PaymentResponse toResponse(Payment payment, String url) {
    return new PaymentResponse(payment.getId(), payment.getEnrollmentId().getId(),
        payment.getTransactionCode(), payment.getMethod().name(), payment.getAmount(), payment.getStatus().name(),
        url, payment.getCreatedAt(), payment.getCompletedAt());
  }

  private long amount(Enrollment enrollment) {
    return enrollment.getAmountDue().setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }

}
