package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.ntt.language_center_management.dto.request.CreatePaymentRequest;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Enrollment;
import com.ntt.language_center_management.entity.Payment;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.enums.EnrollmentPaymentStatus;
import com.ntt.language_center_management.enums.EnrollmentStatus;
import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.enums.PaymentTransactionStatus;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.exception.PaymentGatewayException;
import com.ntt.language_center_management.repository.EnrollmentRepository;
import com.ntt.language_center_management.repository.PaymentRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.service.EnrollmentExpirationService;
import com.ntt.language_center_management.service.impl.PaymentServiceImpl;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.type.TypeReference;

class PaymentServiceImplTest {
  private PaymentRepository payments;
  private EnrollmentRepository enrollments;
  private StudentRepository students;
  private EnrollmentExpirationService expiration;
  private ObjectMapper objectMapper;
  private PaymentServiceImpl service;
  private Student student;
  private Enrollment enrollment;

  @BeforeEach
  void setUp() {
    payments = mock(PaymentRepository.class);
    enrollments = mock(EnrollmentRepository.class);
    students = mock(StudentRepository.class);
    expiration = mock(EnrollmentExpirationService.class);
    objectMapper = mock(ObjectMapper.class);
    service = new PaymentServiceImpl(payments, enrollments, students, objectMapper, expiration);
    student = new Student(7);
    enrollment = enrollment(15, student, "3200000");
    when(students.findByUserId_EmailIgnoreCase("student@example.com")).thenReturn(Optional.of(student));
    when(enrollments.lockById(15)).thenReturn(Optional.of(enrollment));
  }

  @Test
  void shouldStopBeforeLoadingEnrollmentWhenRegistrationExpired() {
    when(expiration.expireIfOverdue(15)).thenReturn(true);

    assertThatThrownBy(() -> service.createPayment(
        new CreatePaymentRequest(15, PaymentMethod.MOMO), principal()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("48 giờ");

    verify(enrollments, never()).lockById(any());
  }

  @Test
  void shouldRejectPaymentForAnotherStudent() {
    enrollment.setStudentId(new Student(99));

    assertThatThrownBy(() -> service.createPayment(
        new CreatePaymentRequest(15, PaymentMethod.MOMO), principal()))
        .isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void shouldRejectCancelledUnconfirmedPaidAndFreeEnrollment() {
    enrollment.setEnrollmentStatus(EnrollmentStatus.CANCELLED);
    assertThatThrownBy(() -> createMomo()).hasMessageContaining("đã bị hủy");

    enrollment.setEnrollmentStatus(EnrollmentStatus.PENDING);
    assertThatThrownBy(() -> createMomo()).hasMessageContaining("không ở trạng thái");

    enrollment.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PAID);
    assertThatThrownBy(() -> createMomo()).hasMessageContaining("đã được thanh toán");

    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PENDING);
    enrollment.setAmountDue(BigDecimal.ZERO);
    assertThatThrownBy(() -> createMomo()).hasMessageContaining("miễn phí");
  }

  @Test
  void shouldRejectMissingGatewayConfigurationWithoutLeakingSecret() {
    ReflectionTestUtils.setField(service, "momoPartnerCode", "CHANGE_ME_PARTNER");

    assertThatThrownBy(this::createMomo)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Thiếu cấu hình MOMO_PARTNER_CODE")
        .hasMessageNotContaining("SECRET");
    verify(payments, never()).save(any());
  }

  @Test
  void shouldRejectLocalhostCallbackUrl() {
    configureMomo();
    ReflectionTestUtils.setField(service, "momoIpnUrl", "http://localhost:8080/api/payments/momo/ipn");

    assertThatThrownBy(this::createMomo)
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("HTTPS public");
  }

  @Test
  void shouldRejectInvalidMomoSignatureWithoutChangingData() {
    configureMomoCallback();
    Map<String, Object> payload = momoPayload(0, "3200000");
    payload.put("signature", "invalid");

    assertThatThrownBy(() -> service.handleMomoIpn(payload))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Chữ ký");

    verify(payments, never()).findByTransactionCodeAndMethod(any(), any());
    verify(enrollments, never()).save(any());
  }

  @Test
  void shouldCompleteMomoPaymentAndEnrollmentWhenCallbackIsValid() {
    configureMomoCallback();
    Payment payment = payment(enrollment, PaymentTransactionStatus.PENDING);
    when(payments.findByTransactionCodeAndMethod("ORDER15", PaymentMethod.MOMO))
        .thenReturn(Optional.of(payment));
    Map<String, Object> payload = signedMomoPayload(0, "3200000");

    Map<String, Object> result = service.handleMomoIpn(payload);

    assertThat(result.get("resultCode")).isEqualTo(0);
    assertThat(payment.getStatus()).isEqualTo(PaymentTransactionStatus.PAID);
    assertThat(payment.getReferenceCode()).isEqualTo("998877");
    assertThat(payment.getCompletedAt()).isNotNull();
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PAID);
    verify(payments).save(payment);
    verify(enrollments).save(enrollment);
  }

  @Test
  void shouldKeepEnrollmentPendingWhenMomoReportsFailure() {
    configureMomoCallback();
    Payment payment = payment(enrollment, PaymentTransactionStatus.PENDING);
    when(payments.findByTransactionCodeAndMethod("ORDER15", PaymentMethod.MOMO))
        .thenReturn(Optional.of(payment));

    service.handleMomoIpn(signedMomoPayload(1006, "3200000"));

    assertThat(payment.getStatus()).isEqualTo(PaymentTransactionStatus.FAILED);
    assertThat(payment.getErrorMessage()).isEqualTo("failed");
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PENDING);
    verify(enrollments, never()).save(any());
  }

  @Test
  void shouldRejectMismatchedAmountAndLeaveEntitiesUnchanged() {
    configureMomoCallback();
    Payment payment = payment(enrollment, PaymentTransactionStatus.PENDING);
    when(payments.findByTransactionCodeAndMethod("ORDER15", PaymentMethod.MOMO))
        .thenReturn(Optional.of(payment));

    assertThatThrownBy(() -> service.handleMomoIpn(signedMomoPayload(0, "1")))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("không khớp");

    assertThat(payment.getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PENDING);
    verify(payments, never()).save(any());
  }

  @Test
  void shouldTreatRepeatedSuccessfulMomoCallbackAsIdempotent() {
    configureMomoCallback();
    Payment payment = payment(enrollment, PaymentTransactionStatus.PAID);
    enrollment.setPaymentStatus(EnrollmentPaymentStatus.PAID);
    when(payments.findByTransactionCodeAndMethod("ORDER15", PaymentMethod.MOMO))
        .thenReturn(Optional.of(payment));

    service.handleMomoIpn(signedMomoPayload(0, "3200000"));

    verify(payments, never()).save(any());
    verify(enrollments, never()).save(any());
  }

  @Test
  void shouldRejectInvalidZaloPayMacWithoutReadingCallbackData() throws Exception {
    ReflectionTestUtils.setField(service, "zaloPayKey2", "ZALO-KEY-2");

    Map<String, Object> result = service.handleZaloPayCallback(
        Map.of("data", "{}", "mac", "invalid"));

    assertThat(result.get("return_code")).isEqualTo(-1);
    verify(objectMapper, never()).readValue(
        org.mockito.ArgumentMatchers.anyString(), any(TypeReference.class));
    verify(payments, never()).save(any());
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldCompleteZaloPayPaymentWhenCallbackMacIsValid() throws Exception {
    ReflectionTestUtils.setField(service, "zaloPayKey2", "ZALO-KEY-2");
    String data = "{\"app_trans_id\":\"260911_LC15\",\"amount\":3200000,\"zp_trans_id\":7788}";
    Map<String, Object> callback = Map.of(
        "app_trans_id", "260911_LC15", "amount", 3_200_000, "zp_trans_id", 7788);
    when(objectMapper.readValue(org.mockito.ArgumentMatchers.eq(data), any(TypeReference.class)))
        .thenReturn(callback);
    Payment payment = payment(enrollment, PaymentTransactionStatus.PENDING);
    payment.setMethod(PaymentMethod.ZALOPAY);
    payment.setTransactionCode("260911_LC15");
    when(payments.findByTransactionCodeAndMethod("260911_LC15", PaymentMethod.ZALOPAY))
        .thenReturn(Optional.of(payment));

    Map<String, Object> result = service.handleZaloPayCallback(
        Map.of("data", data, "mac", hmac(data, "ZALO-KEY-2")));

    assertThat(result.get("return_code")).isEqualTo(1);
    assertThat(payment.getStatus()).isEqualTo(PaymentTransactionStatus.PAID);
    assertThat(payment.getReferenceCode()).isEqualTo("7788");
    assertThat(enrollment.getPaymentStatus()).isEqualTo(EnrollmentPaymentStatus.PAID);
  }

  @Test
  @SuppressWarnings("unchecked")
  void shouldReturnRetryableZaloPayResponseWhenCallbackDataCannotBeProcessed() throws Exception {
    ReflectionTestUtils.setField(service, "zaloPayKey2", "ZALO-KEY-2");
    String data = "{}";
    when(objectMapper.readValue(org.mockito.ArgumentMatchers.eq(data), any(TypeReference.class)))
        .thenThrow(new IllegalArgumentException("invalid callback"));

    Map<String, Object> result = service.handleZaloPayCallback(
        Map.of("data", data, "mac", hmac(data, "ZALO-KEY-2")));

    assertThat(result.get("return_code")).isEqualTo(0);
    assertThat(result.get("return_message")).isEqualTo("invalid callback");
  }

  @Test
  void shouldReturnCurrentStudentPaymentHistory() {
    Payment payment = payment(enrollment, PaymentTransactionStatus.PENDING);
    when(payments.findByEnrollmentId_StudentId_IdOrderByCreatedAtDesc(7))
        .thenReturn(java.util.List.of(payment));

    var result = service.getMyPayments(principal());

    assertThat(result).singleElement().satisfies(value -> {
      assertThat(value.enrollmentId()).isEqualTo(15);
      assertThat(value.method()).isEqualTo("MOMO");
      assertThat(value.status()).isEqualTo("PENDING");
    });
  }

  @Test
  void shouldCreateMomoPaymentWithCorrectAmountAndSignature() {
    ObjectMapper json = new ObjectMapper();
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new PaymentServiceImpl(payments, enrollments, students, json, expiration, builder.build());
    configureMomo();
    when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
      Payment value = invocation.getArgument(0);
      value.setId(5);
      return value;
    });
    server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          var node = json.readTree(body);
          String raw = "accessKey=ACCESS&amount=3200000&extraData="
              + "&ipnUrl=https://backend.example.com/api/payments/momo/ipn"
              + "&orderId=" + node.path("orderId").asText()
              + "&orderInfo=Thanh toan khoa hoc EN-A1-01&partnerCode=PARTNER"
              + "&redirectUrl=https://frontend.example.com/payment-result"
              + "&requestId=" + node.path("requestId").asText()
              + "&requestType=captureWallet";
          assertThat(node.path("amount").asText()).isEqualTo("3200000");
          assertThat(node.path("signature").asText()).isEqualTo(hmac(raw, "SECRET"));
        })
        .andRespond(withSuccess("{\"resultCode\":0,\"payUrl\":\"https://momo/pay\"}",
            MediaType.APPLICATION_JSON));

    var response = service.createPayment(
        new CreatePaymentRequest(15, PaymentMethod.MOMO), principal());

    assertThat(response.paymentUrl()).isEqualTo("https://momo/pay");
    ArgumentCaptor<Payment> captor = ArgumentCaptor.forClass(Payment.class);
    verify(payments).save(captor.capture());
    assertThat(captor.getValue().getStatus()).isEqualTo(PaymentTransactionStatus.PENDING);
    assertThat(captor.getValue().getAmount()).isEqualByComparingTo("3200000");
    server.verify();
  }

  @Test
  void shouldCreateZaloPayPaymentWithCorrectAmountAndMac() {
    ObjectMapper json = new ObjectMapper();
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new PaymentServiceImpl(payments, enrollments, students, json, expiration, builder.build());
    configureZaloPay();
    when(payments.save(any(Payment.class))).thenAnswer(invocation -> {
      Payment value = invocation.getArgument(0);
      value.setId(6);
      return value;
    });
    server.expect(requestTo("https://sandbox.zalopay.vn/v001/tpe/createorder"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          Map<String, String> form = form(body);
          String raw = "2553|" + form.get("app_trans_id") + "|student_7|3200000|"
              + form.get("app_time") + "|{\"redirecturl\":\"https://frontend.example.com/payment-result\"}|[]";
          assertThat(form.get("amount")).isEqualTo("3200000");
          assertThat(form.get("mac")).isEqualTo(hmac(raw, "ZALO-KEY-1"));
        })
        .andRespond(withSuccess("{\"return_code\":1,\"order_url\":\"https://zalo/pay\"}",
            MediaType.APPLICATION_JSON));

    var response = service.createPayment(
        new CreatePaymentRequest(15, PaymentMethod.ZALOPAY), principal());

    assertThat(response.paymentUrl()).isEqualTo("https://zalo/pay");
    assertThat(response.method()).isEqualTo("ZALOPAY");
    server.verify();
  }

  @Test
  void shouldWrapGatewayHttpErrorWithoutExposingRawResponse() {
    ObjectMapper json = new ObjectMapper();
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new PaymentServiceImpl(payments, enrollments, students, json, expiration, builder.build());
    configureMomo();
    server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create"))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON)
            .body("{\"message\":\"invalid partner\",\"secret\":\"must-not-leak\"}"));

    assertThatThrownBy(this::createMomo)
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessageContaining("HTTP 400", "invalid partner")
        .hasMessageNotContaining("must-not-leak");
    verify(payments, never()).save(any());
  }

  @Test
  void shouldWrapGatewayNetworkFailureAndNotCreatePendingPayment() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    service = new PaymentServiceImpl(payments, enrollments, students, new ObjectMapper(),
        expiration, builder.build());
    configureMomo();
    server.expect(requestTo("https://test-payment.momo.vn/v2/gateway/api/create"))
        .andRespond(request -> { throw new java.io.IOException("network unavailable"); });

    assertThatThrownBy(this::createMomo)
        .isInstanceOf(PaymentGatewayException.class)
        .hasMessageContaining("MoMo sandbox");
    verify(payments, never()).save(any());
  }

  @Test
  void shouldRejectMissingMomoSignatureInConstantTimeComparison() {
    configureMomoCallback();
    Map<String, Object> payload = momoPayload(0, "3200000");

    assertThatThrownBy(() -> service.handleMomoIpn(payload))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Chữ ký");
    verify(payments, never()).findByTransactionCodeAndMethod(any(), any());
  }

  private void createMomo() {
    service.createPayment(new CreatePaymentRequest(15, PaymentMethod.MOMO), principal());
  }

  private Principal principal() {
    return () -> "student@example.com";
  }

  private Enrollment enrollment(int id, Student owner, String amount) {
    Enrollment value = new Enrollment(id);
    value.setStudentId(owner);
    value.setEnrollmentStatus(EnrollmentStatus.CONFIRMED);
    value.setPaymentStatus(EnrollmentPaymentStatus.PENDING);
    value.setAmountDue(new BigDecimal(amount));
    Courseclass courseClass = new Courseclass(3);
    courseClass.setClassCode("EN-A1-01");
    value.setCourseClassId(courseClass);
    return value;
  }

  private Payment payment(Enrollment owner, PaymentTransactionStatus status) {
    Payment value = new Payment(5);
    value.setEnrollmentId(owner);
    value.setTransactionCode("ORDER15");
    value.setMethod(PaymentMethod.MOMO);
    value.setAmount(owner.getAmountDue());
    value.setStatus(status);
    return value;
  }

  private void configureMomo() {
    configureMomoCallback();
    ReflectionTestUtils.setField(service, "momoPartnerCode", "PARTNER");
    ReflectionTestUtils.setField(service, "momoEndpoint", "https://test-payment.momo.vn/v2/gateway/api/create");
    ReflectionTestUtils.setField(service, "momoRedirectUrl", "https://frontend.example.com/payment-result");
    ReflectionTestUtils.setField(service, "momoIpnUrl", "https://backend.example.com/api/payments/momo/ipn");
  }

  private void configureMomoCallback() {
    ReflectionTestUtils.setField(service, "momoAccessKey", "ACCESS");
    ReflectionTestUtils.setField(service, "momoSecretKey", "SECRET");
  }

  private void configureZaloPay() {
    ReflectionTestUtils.setField(service, "zaloPayEndpoint",
        "https://sandbox.zalopay.vn/v001/tpe/createorder");
    ReflectionTestUtils.setField(service, "zaloPayAppId", "2553");
    ReflectionTestUtils.setField(service, "zaloPayKey1", "ZALO-KEY-1");
    ReflectionTestUtils.setField(service, "zaloPayCallbackUrl",
        "https://backend.example.com/api/payments/zalopay/callback");
    ReflectionTestUtils.setField(service, "zaloPayRedirectUrl",
        "https://frontend.example.com/payment-result");
  }

  private Map<String, String> form(String body) {
    Map<String, String> values = new java.util.HashMap<>();
    for (String pair : body.split("&")) {
      String[] parts = pair.split("=", 2);
      values.put(java.net.URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
          java.net.URLDecoder.decode(parts.length == 2 ? parts[1] : "", StandardCharsets.UTF_8));
    }
    return values;
  }

  private Map<String, Object> signedMomoPayload(int resultCode, String amount) {
    Map<String, Object> payload = momoPayload(resultCode, amount);
    String raw = "accessKey=ACCESS&amount=" + amount
        + "&extraData=&message=failed&orderId=ORDER15&orderInfo=course"
        + "&orderType=momo_wallet&partnerCode=PARTNER&payType=qr"
        + "&requestId=REQ15&responseTime=1000&resultCode=" + resultCode + "&transId=998877";
    payload.put("signature", hmac(raw, "SECRET"));
    return payload;
  }

  private Map<String, Object> momoPayload(int resultCode, String amount) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("amount", amount);
    payload.put("extraData", "");
    payload.put("message", "failed");
    payload.put("orderId", "ORDER15");
    payload.put("orderInfo", "course");
    payload.put("orderType", "momo_wallet");
    payload.put("partnerCode", "PARTNER");
    payload.put("payType", "qr");
    payload.put("requestId", "REQ15");
    payload.put("responseTime", "1000");
    payload.put("resultCode", resultCode);
    payload.put("transId", "998877");
    return payload;
  }

  private String hmac(String value, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return java.util.HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new AssertionError(exception);
    }
  }
}
