package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.enums.RefundStatus;
import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.enums.PaymentTransactionStatus;

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
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Service
@Transactional
public class BillingServiceImpl implements BillingService {
  private final EnrollmentRepository enrollmentRepository;
  private final PaymentRepository paymentRepository;
  private final RefundRepository refundRepository;
  private final UserRepository userRepository;
  private final RestClient restClient;

  @Value("${payment.momo.refund-endpoint}") private String momoRefundEndpoint;
  @Value("${payment.momo.refund-query-endpoint}") private String momoRefundQueryEndpoint;
  @Value("${payment.momo.partner-code}") private String momoPartnerCode;
  @Value("${payment.momo.access-key}") private String momoAccessKey;
  @Value("${payment.momo.secret-key}") private String momoSecretKey;
  @Value("${payment.zalopay.refund-endpoint}") private String zaloPayRefundEndpoint;
  @Value("${payment.zalopay.refund-query-endpoint}") private String zaloPayRefundQueryEndpoint;
  @Value("${payment.zalopay.app-id}") private String zaloPayAppId;
  @Value("${payment.zalopay.key1}") private String zaloPayKey1;

  public BillingServiceImpl(EnrollmentRepository enrollmentRepository, PaymentRepository paymentRepository,
      RefundRepository refundRepository, UserRepository userRepository) {
    this.enrollmentRepository = enrollmentRepository;
    this.paymentRepository = paymentRepository;
    this.refundRepository = refundRepository;
    this.userRepository = userRepository;
    this.restClient = RestClient.builder().build();
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
        .findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(
            enrollmentId, PaymentTransactionStatus.PAID);
    if (paidPayments.isEmpty() || enrollment.getPaymentStatus() != EnrollmentPaymentStatus.PAID) {
      throw new IllegalArgumentException("Đăng ký chưa có khoản thanh toán thành công để hoàn");
    }
    if (refundRepository.findByEnrollment_IdOrderByCreatedAtDesc(enrollmentId).stream()
        .anyMatch(value -> value.getStatus() == RefundStatus.PENDING)) {
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
    validateRefundGateway(payment);
    Date now = new Date();
    Refund refund = new Refund();
    refund.setEnrollment(enrollment);
    refund.setPayment(payment);
    refund.setProcessedBy(actor);
    refund.setRefundCode(createRefundCode(payment, enrollmentId));
    refund.setIdempotencyKey(request.idempotencyKey().trim());
    refund.setAmount(amount);
    refund.setReason(request.reason().trim());
    refund.setStatus(RefundStatus.PENDING);
    refund.setCreatedAt(now);
    refund = refundRepository.saveAndFlush(refund);

    try {
      if (payment.getMethod() == PaymentMethod.MOMO) submitMomoRefund(refund);
      else submitZaloPayRefund(refund);
    } catch (RuntimeException exception) {
      // Timeout/mất kết nối không chứng minh gateway đã từ chối. Giữ PENDING để query
      // bằng refundCode, tránh Staff gửi lại và tạo hoàn tiền trùng.
      refund.setErrorMessage(gatewayMessage(exception));
      refundRepository.save(refund);
    }
    return refundResponse(refund);
  }

  @Override
  public RefundResponse refreshRefund(Integer refundId, Principal principal) {
    requireStaff(currentUser(principal));
    Refund refund = refundRepository.findById(refundId)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu hoàn tiền"));
    if (refund.getStatus() != RefundStatus.PENDING) return refundResponse(refund);
    try {
      if (refund.getPayment().getMethod() == PaymentMethod.MOMO) queryMomoRefund(refund);
      else queryZaloPayRefund(refund);
    } catch (RuntimeException exception) {
      refund.setErrorMessage(gatewayMessage(exception));
      refundRepository.save(refund);
    }
    return refundResponse(refund);
  }

  private void validateRefundGateway(Payment payment) {
    if (!StringUtils.hasText(payment.getReferenceCode())) {
      throw new IllegalArgumentException("Payment chưa có mã giao dịch từ cổng thanh toán");
    }
    if (payment.getMethod() == PaymentMethod.MOMO) {
      requireConfig(momoPartnerCode, "MOMO_PARTNER_CODE");
      requireConfig(momoAccessKey, "MOMO_ACCESS_KEY");
      requireConfig(momoSecretKey, "MOMO_SECRET_KEY");
      parseLong(payment.getReferenceCode(), "Mã giao dịch MoMo không hợp lệ");
    } else if (payment.getMethod() == PaymentMethod.ZALOPAY) {
      requireConfig(zaloPayAppId, "ZALOPAY_APP_ID");
      requireConfig(zaloPayKey1, "ZALOPAY_KEY1");
      parseLong(payment.getReferenceCode(), "Mã giao dịch ZaloPay không hợp lệ");
    } else {
      throw new IllegalArgumentException("Phương thức thanh toán không hỗ trợ hoàn tiền");
    }
  }

  private void submitMomoRefund(Refund refund) {
    long amount = amount(refund.getAmount());
    String orderId = refund.getRefundCode();
    String requestId = refund.getIdempotencyKey();
    String transId = refund.getPayment().getReferenceCode();
    String description = refund.getReason();
    String raw = "accessKey=" + momoAccessKey + "&amount=" + amount + "&description=" + description
        + "&orderId=" + orderId + "&partnerCode=" + momoPartnerCode
        + "&requestId=" + requestId + "&transId=" + transId;
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("partnerCode", momoPartnerCode);
    body.put("orderId", orderId);
    body.put("requestId", requestId);
    body.put("amount", amount);
    body.put("transId", Long.parseLong(transId));
    body.put("lang", "vi");
    body.put("description", description);
    body.put("signature", hmac(raw, momoSecretKey));
    JsonNode response = restClient.post().uri(momoRefundEndpoint).contentType(MediaType.APPLICATION_JSON)
        .body(body).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("resultCode").asInt(-1);
    String message = response == null ? "MoMo không trả về dữ liệu" : response.path("message").asText();
    if (code == 0) {
      refund.setGatewayRefundId(response.path("transId").asText());
      completeRefund(refund);
    } else if (code == 7002 || code == 1000) {
      refund.setErrorMessage(message);
      refundRepository.save(refund);
    } else failRefund(refund, message);
  }

  private void submitZaloPayRefund(Refund refund) {
    long timestamp = System.currentTimeMillis();
    long amount = amount(refund.getAmount());
    String transId = refund.getPayment().getReferenceCode();
    String description = refund.getReason();
    String macInput = zaloPayAppId + "|" + transId + "|" + amount + "|" + description + "|" + timestamp;
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("app_id", zaloPayAppId);
    form.add("m_refund_id", refund.getRefundCode());
    form.add("zp_trans_id", transId);
    form.add("amount", String.valueOf(amount));
    form.add("timestamp", String.valueOf(timestamp));
    form.add("description", description);
    form.add("mac", hmac(macInput, zaloPayKey1));
    JsonNode response = restClient.post().uri(zaloPayRefundEndpoint)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("return_code").asInt(-1);
    String message = response == null ? "ZaloPay không trả về dữ liệu" : response.path("return_message").asText();
    if (response != null && response.has("refund_id")) {
      refund.setGatewayRefundId(response.path("refund_id").asText());
    }
    if (code == 1) completeRefund(refund);
    else if (code == 3) { refund.setErrorMessage(message); refundRepository.save(refund); }
    else failRefund(refund, message);
  }

  private void queryMomoRefund(Refund refund) {
    String requestId = "QUERY" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String raw = "accessKey=" + momoAccessKey + "&orderId=" + refund.getRefundCode()
        + "&partnerCode=" + momoPartnerCode + "&requestId=" + requestId;
    Map<String, Object> body = Map.of("partnerCode", momoPartnerCode, "requestId", requestId,
        "orderId", refund.getRefundCode(), "lang", "vi", "signature", hmac(raw, momoSecretKey));
    JsonNode response = restClient.post().uri(momoRefundQueryEndpoint).contentType(MediaType.APPLICATION_JSON)
        .body(body).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("resultCode").asInt(-1);
    String message = response == null ? "MoMo không trả về dữ liệu" : response.path("message").asText();
    if (code == 0 && momoQueryContainsSuccess(response, refund)) completeRefund(refund);
    else if (code == 7002 || code == 1000) { refund.setErrorMessage(message); refundRepository.save(refund); }
    else failRefund(refund, message);
  }

  private boolean momoQueryContainsSuccess(JsonNode response, Refund refund) {
    for (JsonNode item : response.path("refundTrans")) {
      if (refund.getRefundCode().equals(item.path("orderId").asText())
          && item.path("resultCode").asInt(-1) == 0) {
        if (item.has("transId")) refund.setGatewayRefundId(item.path("transId").asText());
        return true;
      }
    }
    return !response.has("refundTrans");
  }

  private void queryZaloPayRefund(Refund refund) {
    long timestamp = System.currentTimeMillis();
    String macInput = zaloPayAppId + "|" + refund.getRefundCode() + "|" + timestamp;
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("app_id", zaloPayAppId);
    form.add("m_refund_id", refund.getRefundCode());
    form.add("timestamp", String.valueOf(timestamp));
    form.add("mac", hmac(macInput, zaloPayKey1));
    JsonNode response = restClient.post().uri(zaloPayRefundQueryEndpoint)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("return_code").asInt(-1);
    String message = response == null ? "ZaloPay không trả về dữ liệu" : response.path("return_message").asText();
    if (code == 1) completeRefund(refund);
    else if (code == 3) { refund.setErrorMessage(message); refundRepository.save(refund); }
    else failRefund(refund, message);
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
      Enrollment enrollment = refund.getEnrollment();
      enrollment.setPaymentStatus(EnrollmentPaymentStatus.REFUNDED);
      enrollment.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
      enrollment.setCancelledAt(now);
      enrollment.setCancellationReason(refund.getReason());
      enrollmentRepository.save(enrollment);
    }
  }

  private void failRefund(Refund refund, String message) {
    refund.setStatus(RefundStatus.FAILED);
    refund.setErrorMessage(message);
    refundRepository.save(refund);
  }

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
    return refundRepository.findByEnrollment_IdOrderByCreatedAtDesc(enrollmentId).stream()
        .filter(value -> value.getStatus() == RefundStatus.COMPLETED).map(Refund::getAmount)
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
      payment.getEnrollmentId().getId(), payment.getTransactionCode(), payment.getMethod().name(), payment.getAmount(),
      payment.getStatus().name(), null, payment.getCreatedAt(), payment.getCompletedAt()); }
  private RefundResponse refundResponse(Refund refund) { return new RefundResponse(refund.getId(),
      refund.getEnrollment().getId(), refund.getPayment().getId(), refund.getRefundCode(),
      refund.getPayment().getMethod().name(), refund.getEnrollment().getStudentId().getUserId().getFullName(),
      refund.getEnrollment().getCourseClassId().getClassName(), refund.getAmount(),
      refund.getStatus().name(), refund.getGatewayRefundId(), refund.getErrorMessage(), refund.getReason(),
      refund.getProcessedBy().getId(), refund.getProcessedBy().getFullName(),
      refund.getCreatedAt(), refund.getCompletedAt()); }

  private long amount(BigDecimal value) {
    return value.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }
  private String createRefundCode(Payment payment, Integer enrollmentId) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    if (payment.getMethod() == PaymentMethod.ZALOPAY) {
      return java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
          .format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
          + "_" + zaloPayAppId + "_RF" + enrollmentId + suffix;
    }
    return "RF" + enrollmentId + suffix;
  }
  private long parseLong(String value, String message) {
    try { return Long.parseLong(value); }
    catch (NumberFormatException exception) { throw new IllegalArgumentException(message); }
  }
  private void requireConfig(String value, String name) {
    if (!StringUtils.hasText(value) || value.startsWith("CHANGE_ME")) {
      throw new IllegalArgumentException("Thiếu cấu hình " + name);
    }
  }
  private String hmac(String value, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Không thể tạo chữ ký hoàn tiền", exception);
    }
  }
  private String gatewayMessage(RuntimeException exception) {
    return StringUtils.hasText(exception.getMessage()) ? exception.getMessage() : "Không thể kết nối cổng thanh toán";
  }
}
