package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.CreatePaymentRequest;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.service.PaymentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
  private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final PaymentRepository paymentRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final StudentRepository studentRepository;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;

  @Value("${payment.momo.endpoint}") private String momoEndpoint;
  @Value("${payment.momo.partner-code}") private String momoPartnerCode;
  @Value("${payment.momo.access-key}") private String momoAccessKey;
  @Value("${payment.momo.secret-key}") private String momoSecretKey;
  @Value("${payment.momo.redirect-url}") private String momoRedirectUrl;
  @Value("${payment.momo.ipn-url}") private String momoIpnUrl;
  @Value("${payment.zalopay.endpoint}") private String zaloPayEndpoint;
  @Value("${payment.zalopay.app-id}") private String zaloPayAppId;
  @Value("${payment.zalopay.key1}") private String zaloPayKey1;
  @Value("${payment.zalopay.key2}") private String zaloPayKey2;
  @Value("${payment.zalopay.callback-url}") private String zaloPayCallbackUrl;
  @Value("${payment.zalopay.redirect-url}") private String zaloPayRedirectUrl;

  public PaymentServiceImpl(
      PaymentRepository paymentRepository,
      EnrollmentRepository enrollmentRepository,
      StudentRepository studentRepository,
      ObjectMapper objectMapper) {
    this.paymentRepository = paymentRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.studentRepository = studentRepository;
    this.objectMapper = objectMapper;
    this.restClient = RestClient.builder().build();
  }

  @Override
  public PaymentResponse createPayment(CreatePaymentRequest request, Principal principal) {
    Student student = currentStudent(principal);
    Enrollment enrollment = enrollmentRepository.lockById(request.enrollmentId())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đăng ký"));
    if (!enrollment.getStudentId().getId().equals(student.getId())) {
      throw new UnauthorizedException("Bạn không được thanh toán đăng ký của học viên khác");
    }
    if ("CANCELLED".equals(enrollment.getEnrollmentStatus())) {
      throw new IllegalArgumentException("Đăng ký đã bị hủy");
    }
    if ("PAID".equals(enrollment.getPaymentStatus())) {
      throw new IllegalArgumentException("Đăng ký đã được thanh toán");
    }
    if (enrollment.getAmountDue() == null || enrollment.getAmountDue().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Đăng ký miễn phí không cần tạo giao dịch thanh toán");
    }

    String method = request.method().toUpperCase();
    return "MOMO".equals(method)
        ? createMomo(enrollment, student)
        : createZaloPay(enrollment, student);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PaymentResponse> getMyPayments(Principal principal) {
    Student student = currentStudent(principal);
    return paymentRepository.findByEnrollmentId_StudentId_IdOrderByCreatedAtDesc(student.getId())
        .stream().map(payment -> toResponse(payment, null)).toList();
  }

  private PaymentResponse createMomo(Enrollment enrollment, Student student) {
    requireConfig(momoPartnerCode, "MOMO_PARTNER_CODE");
    requireConfig(momoAccessKey, "MOMO_ACCESS_KEY");
    requireConfig(momoSecretKey, "MOMO_SECRET_KEY");
    long amount = amount(enrollment);
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String orderId = "LCMOMO" + enrollment.getId() + suffix;
    String requestId = "REQ" + suffix;
    String orderInfo = "Thanh toan khoa hoc " + enrollment.getCourseClassId().getClassCode();
    String extraData = "";
    String raw = "accessKey=" + momoAccessKey + "&amount=" + amount + "&extraData=" + extraData
        + "&ipnUrl=" + momoIpnUrl + "&orderId=" + orderId + "&orderInfo=" + orderInfo
        + "&partnerCode=" + momoPartnerCode + "&redirectUrl=" + momoRedirectUrl
        + "&requestId=" + requestId + "&requestType=captureWallet";

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("partnerCode", momoPartnerCode);
    body.put("partnerName", "Lingua Center");
    body.put("storeId", "LinguaCenter");
    body.put("requestType", "captureWallet");
    body.put("ipnUrl", momoIpnUrl);
    body.put("redirectUrl", momoRedirectUrl);
    body.put("orderId", orderId);
    body.put("amount", String.valueOf(amount));
    body.put("orderInfo", orderInfo);
    body.put("requestId", requestId);
    body.put("extraData", extraData);
    body.put("lang", "vi");
    body.put("signature", hmac(raw, momoSecretKey));

    JsonNode response = restClient.post().uri(momoEndpoint).contentType(MediaType.APPLICATION_JSON)
        .body(body).retrieve().body(JsonNode.class);
    if (response == null || response.path("resultCode").asInt(-1) != 0
        || !StringUtils.hasText(response.path("payUrl").asText())) {
      throw new IllegalArgumentException("Không tạo được giao dịch MoMo: "
          + (response == null ? "không có phản hồi" : response.path("message").asText()));
    }
    Payment payment = savePending(enrollment, orderId, "MOMO");
    return toResponse(payment, response.path("payUrl").asText());
  }

  private PaymentResponse createZaloPay(Enrollment enrollment, Student student) {
    requireConfig(zaloPayAppId, "ZALOPAY_APP_ID");
    requireConfig(zaloPayKey1, "ZALOPAY_KEY1");
    long now = System.currentTimeMillis();
    long amount = amount(enrollment);
    String prefix = LocalDate.now(VIETNAM_ZONE).format(DateTimeFormatter.ofPattern("yyMMdd"));
    String transactionId = prefix + "_LC" + enrollment.getId() + now % 1_000_000_000L;
    String appUser = "student_" + student.getId();
    String embedData = "{\"redirecturl\":\"" + zaloPayRedirectUrl + "\"}";
    String items = "[]";
    String macInput = zaloPayAppId + "|" + transactionId + "|" + appUser + "|" + amount
        + "|" + now + "|" + embedData + "|" + items;

    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("app_id", zaloPayAppId);
    form.add("app_user", appUser);
    form.add("app_trans_id", transactionId);
    form.add("app_time", String.valueOf(now));
    form.add("amount", String.valueOf(amount));
    form.add("item", items);
    form.add("embed_data", embedData);
    form.add("description", "Thanh toan khoa hoc " + enrollment.getCourseClassId().getClassCode());
    form.add("bank_code", "");
    form.add("callback_url", zaloPayCallbackUrl);
    form.add("mac", hmac(macInput, zaloPayKey1));

    JsonNode response = restClient.post().uri(zaloPayEndpoint)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
        .retrieve().body(JsonNode.class);
    if (response == null || response.path("return_code").asInt(-1) != 1
        || !StringUtils.hasText(response.path("order_url").asText())) {
      throw new IllegalArgumentException("Không tạo được giao dịch ZaloPay: "
          + (response == null ? "không có phản hồi" : response.path("return_message").asText()));
    }
    Payment payment = savePending(enrollment, transactionId, "ZALOPAY");
    return toResponse(payment, response.path("order_url").asText());
  }

  @Override
  public Map<String, Object> handleMomoIpn(Map<String, Object> payload) {
    requireConfig(momoAccessKey, "MOMO_ACCESS_KEY");
    requireConfig(momoSecretKey, "MOMO_SECRET_KEY");
    String signature = text(payload.get("signature"));
    String raw = "accessKey=" + momoAccessKey + "&amount=" + text(payload.get("amount"))
        + "&extraData=" + value(payload.get("extraData")) + "&message=" + text(payload.get("message"))
        + "&orderId=" + text(payload.get("orderId")) + "&orderInfo=" + text(payload.get("orderInfo"))
        + "&orderType=" + text(payload.get("orderType")) + "&partnerCode=" + text(payload.get("partnerCode"))
        + "&payType=" + text(payload.get("payType")) + "&requestId=" + text(payload.get("requestId"))
        + "&responseTime=" + text(payload.get("responseTime")) + "&resultCode=" + text(payload.get("resultCode"))
        + "&transId=" + text(payload.get("transId"));
    if (!constantEquals(signature, hmac(raw, momoSecretKey))) {
      throw new IllegalArgumentException("Chữ ký callback MoMo không hợp lệ");
    }
    Payment payment = findPayment(text(payload.get("orderId")), "MOMO");
    int resultCode = Integer.parseInt(text(payload.get("resultCode")));
    if (resultCode == 0) complete(payment, Long.parseLong(text(payload.get("amount"))), text(payload.get("transId")));
    else fail(payment, text(payload.get("message")));
    return Map.of("resultCode", 0, "message", "Received");
  }

  @Override
  public Map<String, Object> handleZaloPayCallback(Map<String, Object> payload) {
    requireConfig(zaloPayKey2, "ZALOPAY_KEY2");
    String data = text(payload.get("data"));
    if (!constantEquals(text(payload.get("mac")), hmac(data, zaloPayKey2))) {
      return Map.of("return_code", -1, "return_message", "invalid signature");
    }
    try {
      Map<String, Object> callback = objectMapper.readValue(data, new TypeReference<>() {});
      Payment payment = findPayment(text(callback.get("app_trans_id")), "ZALOPAY");
      complete(payment, Long.parseLong(text(callback.get("amount"))), text(callback.get("zp_trans_id")));
      return Map.of("return_code", 1, "return_message", "success");
    } catch (Exception exception) {
      return Map.of("return_code", 0, "return_message", exception.getMessage());
    }
  }

  private Payment savePending(Enrollment enrollment, String code, String method) {
    Payment payment = new Payment();
    payment.setEnrollmentId(enrollment);
    payment.setTransactionCode(code);
    payment.setMethod(method);
    payment.setAmount(enrollment.getAmountDue());
    payment.setStatus("PENDING");
    payment.setCreatedAt(new Date());
    return paymentRepository.save(payment);
  }

  private void complete(Payment payment, long paidAmount, String reference) {
    if ("PAID".equals(payment.getStatus())) return;
    if (paidAmount != amount(payment.getEnrollmentId())) throw new IllegalArgumentException("Số tiền thanh toán không khớp");
    Enrollment enrollment = payment.getEnrollmentId();
    if ("CANCELLED".equals(enrollment.getEnrollmentStatus())) throw new IllegalArgumentException("Đăng ký đã hủy");
    Date now = new Date();
    payment.setStatus("PAID");
    payment.setCompletedAt(now);
    payment.setReferenceCode(reference);
    enrollment.setEnrollmentStatus("CONFIRMED");
    enrollment.setPaymentStatus("PAID");
    if (enrollment.getConfirmedAt() == null) enrollment.setConfirmedAt(now);
    paymentRepository.save(payment);
    enrollmentRepository.save(enrollment);
  }

  private void fail(Payment payment, String message) {
    if (!"PAID".equals(payment.getStatus())) {
      payment.setStatus("FAILED");
      payment.setErrorMessage(message);
      paymentRepository.save(payment);
    }
  }

  private Payment findPayment(String code, String method) {
    return paymentRepository.findByTransactionCodeAndMethod(code, method)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
  }

  private Student currentStudent(Principal principal) {
    if (principal == null || !StringUtils.hasText(principal.getName())) throw new UnauthorizedException("Chưa đăng nhập");
    return studentRepository.findByUserId_EmailIgnoreCase(principal.getName())
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy học viên"));
  }

  private PaymentResponse toResponse(Payment payment, String url) {
    return new PaymentResponse(payment.getId(), payment.getEnrollmentId().getId(),
        payment.getTransactionCode(), payment.getMethod(), payment.getAmount(), payment.getStatus(),
        url, payment.getCreatedAt(), payment.getCompletedAt());
  }

  private long amount(Enrollment enrollment) {
    return enrollment.getAmountDue().setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }

  private void requireConfig(String value, String name) {
    if (!StringUtils.hasText(value) || value.startsWith("CHANGE_ME")) throw new IllegalArgumentException("Thiếu cấu hình " + name);
  }

  private String hmac(String value, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Không thể tạo chữ ký thanh toán", exception);
    }
  }

  private boolean constantEquals(String left, String right) {
    return left != null && java.security.MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }
  private String text(Object value) { return value == null ? "" : String.valueOf(value); }
  private String value(Object value) { return value == null ? "" : String.valueOf(value); }
}
