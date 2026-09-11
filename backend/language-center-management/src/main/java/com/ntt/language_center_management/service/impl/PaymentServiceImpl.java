package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.enums.PaymentMethod;

import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.enums.PaymentTransactionStatus;

import com.ntt.language_center_management.dto.request.CreatePaymentRequest;
import com.ntt.language_center_management.dto.response.PaymentResponse;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.PaymentGatewayException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.transaction.TransactionExecutor;
import com.ntt.language_center_management.service.PaymentService;
import com.ntt.language_center_management.service.EnrollmentExpirationService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.security.Principal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static com.ntt.language_center_management.util.PaymentGatewayUtils.constantEquals;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.hmacSha256;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.requireConfig;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.stringValue;

@Service
public class PaymentServiceImpl implements PaymentService {
  private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

  private final PaymentRepository paymentRepository;
  private final EnrollmentRepository enrollmentRepository;
  private final CurrentUserResolver currentUserResolver;
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  private final EnrollmentExpirationService enrollmentExpirationService;
  private final TransactionExecutor transactionExecutor;

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

  @Autowired
  public PaymentServiceImpl(
      PaymentRepository paymentRepository,
      EnrollmentRepository enrollmentRepository,
      CurrentUserResolver currentUserResolver,
      ObjectMapper objectMapper,
      EnrollmentExpirationService enrollmentExpirationService,
      TransactionExecutor transactionExecutor) {
    this(paymentRepository, enrollmentRepository, currentUserResolver, objectMapper,
        enrollmentExpirationService, transactionExecutor, RestClient.builder().build());
  }

  public PaymentServiceImpl(
      PaymentRepository paymentRepository,
      EnrollmentRepository enrollmentRepository,
      CurrentUserResolver currentUserResolver,
      ObjectMapper objectMapper,
      EnrollmentExpirationService enrollmentExpirationService,
      TransactionExecutor transactionExecutor,
      RestClient restClient) {
    this.paymentRepository = paymentRepository;
    this.enrollmentRepository = enrollmentRepository;
    this.currentUserResolver = currentUserResolver;
    this.objectMapper = objectMapper;
    this.enrollmentExpirationService = enrollmentExpirationService;
    this.transactionExecutor = transactionExecutor;
    this.restClient = restClient;
  }

  @Override
  public PaymentResponse createPayment(CreatePaymentRequest request, Principal principal) {
    PaymentPreparation preparation = transactionExecutor.required(
        () -> preparePayment(request, principal));
    return preparation.method() == PaymentMethod.MOMO
        ? createMomo(preparation.enrollment(), preparation.student())
        : createZaloPay(preparation.enrollment(), preparation.student());
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

    return new PaymentPreparation(enrollment, student, request.method());
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
    requirePublicCallback(momoIpnUrl, "MOMO_IPN_URL");
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
    body.put("signature", hmacSha256(raw, momoSecretKey));

    JsonNode response;
    try {
      response = restClient.post().uri(momoEndpoint).contentType(MediaType.APPLICATION_JSON)
          .body(body).retrieve().body(JsonNode.class);
    } catch (RestClientResponseException exception) {
      throw gatewayRejected("MoMo", exception);
    } catch (ResourceAccessException exception) {
      throw new PaymentGatewayException(
          "Không kết nối được MoMo sandbox. Vui lòng kiểm tra mạng và MOMO_ENDPOINT.", exception);
    }
    if (response == null || response.path("resultCode").asInt(-1) != 0
        || !StringUtils.hasText(response.path("payUrl").asText())) {
      throw new IllegalArgumentException("Không tạo được giao dịch MoMo: "
          + (response == null ? "không có phản hồi" : response.path("message").asText()));
    }
    Payment payment = savePending(enrollment, orderId, PaymentMethod.MOMO);
    return toResponse(payment, response.path("payUrl").asText());
  }

  private PaymentResponse createZaloPay(Enrollment enrollment, Student student) {
    requireConfig(zaloPayAppId, "ZALOPAY_APP_ID");
    requireConfig(zaloPayKey1, "ZALOPAY_KEY1");
    requirePublicCallback(zaloPayCallbackUrl, "ZALOPAY_CALLBACK_URL");
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
    form.add("mac", hmacSha256(macInput, zaloPayKey1));

    JsonNode response;
    try {
      response = restClient.post().uri(zaloPayEndpoint)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
          .retrieve().body(JsonNode.class);
    } catch (RestClientResponseException exception) {
      throw gatewayRejected("ZaloPay", exception);
    } catch (ResourceAccessException exception) {
      throw new PaymentGatewayException(
          "Không kết nối được ZaloPay sandbox. Vui lòng kiểm tra mạng và ZALOPAY_ENDPOINT.", exception);
    }
    if (response == null || response.path("return_code").asInt(-1) != 1
        || !StringUtils.hasText(response.path("order_url").asText())) {
      throw new IllegalArgumentException("Không tạo được giao dịch ZaloPay: "
          + (response == null ? "không có phản hồi" : response.path("return_message").asText()));
    }
    Payment payment = savePending(enrollment, transactionId, PaymentMethod.ZALOPAY);
    return toResponse(payment, response.path("order_url").asText());
  }

  @Override
  @Transactional
  public Map<String, Object> handleMomoIpn(Map<String, Object> payload) {
    requireConfig(momoAccessKey, "MOMO_ACCESS_KEY");
    requireConfig(momoSecretKey, "MOMO_SECRET_KEY");
    String signature = stringValue(payload.get("signature"));
    String raw = "accessKey=" + momoAccessKey + "&amount=" + stringValue(payload.get("amount"))
        + "&extraData=" + stringValue(payload.get("extraData")) + "&message=" + stringValue(payload.get("message"))
        + "&orderId=" + stringValue(payload.get("orderId")) + "&orderInfo=" + stringValue(payload.get("orderInfo"))
        + "&orderType=" + stringValue(payload.get("orderType")) + "&partnerCode=" + stringValue(payload.get("partnerCode"))
        + "&payType=" + stringValue(payload.get("payType")) + "&requestId=" + stringValue(payload.get("requestId"))
        + "&responseTime=" + stringValue(payload.get("responseTime")) + "&resultCode=" + stringValue(payload.get("resultCode"))
        + "&transId=" + stringValue(payload.get("transId"));
    if (!constantEquals(signature, hmacSha256(raw, momoSecretKey))) {
      throw new IllegalArgumentException("Chữ ký callback MoMo không hợp lệ");
    }
    Payment payment = findPayment(stringValue(payload.get("orderId")), PaymentMethod.MOMO);
    int resultCode = Integer.parseInt(stringValue(payload.get("resultCode")));
    if (resultCode == 0) complete(payment, Long.parseLong(stringValue(payload.get("amount"))), stringValue(payload.get("transId")));
    else fail(payment, stringValue(payload.get("message")));
    return Map.of("resultCode", 0, "message", "Received");
  }

  @Override
  @Transactional
  public Map<String, Object> handleZaloPayCallback(Map<String, Object> payload) {
    requireConfig(zaloPayKey2, "ZALOPAY_KEY2");
    String data = stringValue(payload.get("data"));
    if (!constantEquals(stringValue(payload.get("mac")), hmacSha256(data, zaloPayKey2))) {
      return Map.of("return_code", -1, "return_message", "invalid signature");
    }
    try {
      Map<String, Object> callback = objectMapper.readValue(data, new TypeReference<>() {});
      Payment payment = findPayment(stringValue(callback.get("app_trans_id")), PaymentMethod.ZALOPAY);
      complete(payment, Long.parseLong(stringValue(callback.get("amount"))), stringValue(callback.get("zp_trans_id")));
      return Map.of("return_code", 1, "return_message", "success");
    } catch (Exception exception) {
      return Map.of("return_code", 0, "return_message", exception.getMessage());
    }
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
    if (enrollment.getEnrollmentStatus() == EnrollmentStatus.CANCELLED) throw new IllegalArgumentException("Đăng ký đã hủy");
    if (enrollment.getEnrollmentStatus() != EnrollmentStatus.CONFIRMED) throw new IllegalArgumentException("Đăng ký không còn hiệu lực");
    Date now = new Date();
    payment.setStatus(PaymentTransactionStatus.PAID);
    payment.setCompletedAt(now);
    payment.setReferenceCode(reference);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PAID);
    if (enrollment.getConfirmedAt() == null) enrollment.setConfirmedAt(now);
    paymentRepository.save(payment);
    enrollmentRepository.save(enrollment);
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
      Enrollment enrollment, Student student, PaymentMethod method) {}

  private PaymentResponse toResponse(Payment payment, String url) {
    return new PaymentResponse(payment.getId(), payment.getEnrollmentId().getId(),
        payment.getTransactionCode(), payment.getMethod().name(), payment.getAmount(), payment.getStatus().name(),
        url, payment.getCreatedAt(), payment.getCompletedAt());
  }

  private long amount(Enrollment enrollment) {
    return enrollment.getAmountDue().setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }

  private void requirePublicCallback(String value, String name) {
    requireConfig(value, name);
    try {
      URI uri = URI.create(value);
      String host = uri.getHost();
      if (!"https".equalsIgnoreCase(uri.getScheme()) || !StringUtils.hasText(host)
          || "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)
          || host.toLowerCase().contains("your_public_backend")) {
        throw new IllegalArgumentException(
            name + " phải là URL HTTPS public trỏ tới backend (dùng ngrok hoặc URL deploy), không dùng localhost/YOUR_PUBLIC_BACKEND");
      }
    } catch (IllegalArgumentException exception) {
      if (exception.getMessage() != null && exception.getMessage().startsWith(name)) throw exception;
      throw new IllegalArgumentException(name + " không phải URL hợp lệ");
    }
  }

  private PaymentGatewayException gatewayRejected(
      String gateway, RestClientResponseException exception) {
    String detail = gatewayMessage(exception.getResponseBodyAsString());
    String message = gateway + " sandbox từ chối yêu cầu (HTTP "
        + exception.getStatusCode().value() + ")";
    if (StringUtils.hasText(detail)) message += ": " + detail;
    return new PaymentGatewayException(message, exception);
  }

  private String gatewayMessage(String responseBody) {
    if (!StringUtils.hasText(responseBody)) return null;
    try {
      JsonNode body = objectMapper.readTree(responseBody);
      for (String field : List.of("message", "return_message", "sub_return_message")) {
        String value = body.path(field).asText();
        if (StringUtils.hasText(value)) return value;
      }
    } catch (Exception ignored) {
      // A non-JSON gateway body is intentionally not returned to the client.
    }
    return null;
  }

}
