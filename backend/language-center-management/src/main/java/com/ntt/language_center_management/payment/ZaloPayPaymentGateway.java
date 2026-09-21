package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.exception.PaymentGatewayException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.constantEquals;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.hmacSha256;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.requireConfig;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.stringValue;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ZaloPayPaymentGateway implements PaymentGateway {
  private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  @Value("${payment.zalopay.endpoint}") private String zaloPayEndpoint;
  @Value("${payment.zalopay.app-id}") private String zaloPayAppId;
  @Value("${payment.zalopay.key1}") private String zaloPayKey1;
  @Value("${payment.zalopay.key2}") private String zaloPayKey2;
  @Value("${payment.zalopay.callback-url}") private String zaloPayCallbackUrl;
  @Value("${payment.zalopay.redirect-url}") private String zaloPayRedirectUrl;

  @Autowired
  public ZaloPayPaymentGateway(ObjectMapper objectMapper) {
    this(objectMapper, RestClient.builder().build());
  }

  public ZaloPayPaymentGateway(ObjectMapper objectMapper, RestClient restClient) {
    this.objectMapper = objectMapper;
    this.restClient = restClient;
  }

  @Override public PaymentMethod method() { return PaymentMethod.ZALOPAY; }

  @Override
  public PaymentCheckout createPayment(PaymentCommand command) {
    requireConfig(zaloPayAppId, "ZALOPAY_APP_ID");
    requireConfig(zaloPayKey1, "ZALOPAY_KEY1");
    GatewaySupport.requirePublicCallback(zaloPayCallbackUrl, "ZALOPAY_CALLBACK_URL");
    long now = System.currentTimeMillis();
    long amount = command.amount();
    String prefix = LocalDate.now(VIETNAM_ZONE).format(DateTimeFormatter.ofPattern("yyMMdd"));
    String transactionId = prefix + "_LC" + command.enrollmentId() + now % 1_000_000_000L;
    String appUser = "student_" + command.studentId();
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
    form.add("description", "Thanh toan khoa hoc " + command.classCode());
    form.add("bank_code", "");
    form.add("callback_url", zaloPayCallbackUrl);
    form.add("mac", hmacSha256(macInput, zaloPayKey1));

    JsonNode response;
    try {
      response = restClient.post().uri(zaloPayEndpoint)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
          .retrieve().body(JsonNode.class);
    } catch (RestClientResponseException exception) {
      throw GatewaySupport.gatewayRejected(objectMapper, "ZaloPay", exception);
    } catch (ResourceAccessException exception) {
      throw new PaymentGatewayException(
          "Không kết nối được ZaloPay sandbox. Vui lòng kiểm tra mạng và ZALOPAY_ENDPOINT.", exception);
    }
    if (response == null || response.path("return_code").asInt(-1) != 1
        || !StringUtils.hasText(response.path("order_url").asText())) {
      throw new IllegalArgumentException("Không tạo được giao dịch ZaloPay: "
          + (response == null ? "không có phản hồi" : response.path("return_message").asText()));
    }
    return new PaymentCheckout(transactionId, response.path("order_url").asText());
  }

  @Override
  public Map<String, Object> handleCallback(Map<String, Object> payload, Consumer<PaymentCallback> completion) {
    requireConfig(zaloPayKey2, "ZALOPAY_KEY2");
    String data = stringValue(payload.get("data"));
    if (!constantEquals(stringValue(payload.get("mac")), hmacSha256(data, zaloPayKey2))) {
      return Map.of("return_code", -1, "return_message", "invalid signature");
    }
    try {
      Map<String, Object> callback = objectMapper.readValue(data, new TypeReference<>() {});
      completion.accept(new PaymentCallback(stringValue(callback.get("app_trans_id")), true,
          stringValue(callback.get("amount")), stringValue(callback.get("zp_trans_id")), null));
      return Map.of("return_code", 1, "return_message", "success");
    } catch (Exception exception) {
      return Map.of("return_code", 0, "return_message", exception.getMessage());
    }
  }
}
