package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.exception.PaymentGatewayException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.constantEquals;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.hmacSha256;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.requireConfig;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.stringValue;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class MomoPaymentGateway implements PaymentGateway {
  private final ObjectMapper objectMapper;
  private final RestClient restClient;
  @Value("${payment.momo.endpoint}") private String momoEndpoint;
  @Value("${payment.momo.partner-code}") private String momoPartnerCode;
  @Value("${payment.momo.access-key}") private String momoAccessKey;
  @Value("${payment.momo.secret-key}") private String momoSecretKey;
  @Value("${payment.momo.redirect-url}") private String momoRedirectUrl;
  @Value("${payment.momo.ipn-url}") private String momoIpnUrl;

  @Autowired
  public MomoPaymentGateway(ObjectMapper objectMapper) {
    this(objectMapper, RestClient.builder().build());
  }

  public MomoPaymentGateway(ObjectMapper objectMapper, RestClient restClient) {
    this.objectMapper = objectMapper;
    this.restClient = restClient;
  }

  @Override public PaymentMethod method() { return PaymentMethod.MOMO; }

  @Override
  public PaymentCheckout createPayment(PaymentCommand command) {
    requireConfig(momoPartnerCode, "MOMO_PARTNER_CODE");
    requireConfig(momoAccessKey, "MOMO_ACCESS_KEY");
    requireConfig(momoSecretKey, "MOMO_SECRET_KEY");
    GatewaySupport.requirePublicCallback(momoIpnUrl, "MOMO_IPN_URL");
    long amount = command.amount();
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String orderId = "LCMOMO" + command.enrollmentId() + suffix;
    String requestId = "REQ" + suffix;
    String orderInfo = "Thanh toan khoa hoc " + command.classCode();
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
      throw GatewaySupport.gatewayRejected(objectMapper, "MoMo", exception);
    } catch (ResourceAccessException exception) {
      throw new PaymentGatewayException(
          "Không kết nối được MoMo sandbox. Vui lòng kiểm tra mạng và MOMO_ENDPOINT.", exception);
    }
    if (response == null || response.path("resultCode").asInt(-1) != 0
        || !StringUtils.hasText(response.path("payUrl").asText())) {
      throw new IllegalArgumentException("Không tạo được giao dịch MoMo: "
          + (response == null ? "không có phản hồi" : response.path("message").asText()));
    }
    return new PaymentCheckout(orderId, response.path("payUrl").asText());
  }

  @Override
  public Map<String, Object> handleCallback(Map<String, Object> payload, Consumer<PaymentCallback> completion) {
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
    int resultCode = Integer.parseInt(stringValue(payload.get("resultCode")));
    completion.accept(new PaymentCallback(stringValue(payload.get("orderId")), resultCode == 0,
        stringValue(payload.get("amount")), stringValue(payload.get("transId")),
        stringValue(payload.get("message"))));
    return Map.of("resultCode", 0, "message", "Received");
  }
}
