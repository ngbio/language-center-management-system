package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.enums.RefundStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.hmacSha256;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.requireConfig;
import tools.jackson.databind.JsonNode;

@Service
public class MomoRefundGateway implements RefundGateway {
  private final RestClient restClient;
  @Value("${payment.momo.refund-endpoint}") private String momoRefundEndpoint;
  @Value("${payment.momo.refund-query-endpoint}") private String momoRefundQueryEndpoint;
  @Value("${payment.momo.partner-code}") private String momoPartnerCode;
  @Value("${payment.momo.access-key}") private String momoAccessKey;
  @Value("${payment.momo.secret-key}") private String momoSecretKey;

  @Autowired
  public MomoRefundGateway() { this(RestClient.builder().build()); }

  public MomoRefundGateway(RestClient restClient) { this.restClient = restClient; }

  @Override public PaymentMethod method() { return PaymentMethod.MOMO; }

  @Override
  public void validate(String referenceCode) {
    if (!StringUtils.hasText(referenceCode)) {
      throw new IllegalArgumentException("Payment chưa có mã giao dịch từ cổng thanh toán");
    }
    requireConfig(momoPartnerCode, "MOMO_PARTNER_CODE");
    requireConfig(momoAccessKey, "MOMO_ACCESS_KEY");
    requireConfig(momoSecretKey, "MOMO_SECRET_KEY");
    parseLong(referenceCode, "Mã giao dịch MoMo không hợp lệ");
  }

  @Override
  public String createRefundCode(Integer enrollmentId) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    return "RF" + enrollmentId + suffix;
  }

  @Override
  public RefundResult submit(RefundCommand command) {
    long amount = amount(command.amount());
    String orderId = command.refundCode();
    String requestId = command.idempotencyKey();
    String transId = command.referenceCode();
    String description = command.reason();
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
    body.put("signature", hmacSha256(raw, momoSecretKey));
    JsonNode response = restClient.post().uri(momoRefundEndpoint).contentType(MediaType.APPLICATION_JSON)
        .body(body).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("resultCode").asInt(-1);
    String message = response == null ? "MoMo không trả về dữ liệu" : response.path("message").asText();
    return new RefundResult(code == 0 ? RefundStatus.COMPLETED
        : (code == 7002 || code == 1000) ? RefundStatus.PENDING : RefundStatus.FAILED,
        code == 0 ? response.path("transId").asText() : null, message);
  }

  @Override
  public RefundResult query(RefundCommand command) {
    String requestId = "QUERY" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    String raw = "accessKey=" + momoAccessKey + "&orderId=" + command.refundCode()
        + "&partnerCode=" + momoPartnerCode + "&requestId=" + requestId;
    Map<String, Object> body = Map.of("partnerCode", momoPartnerCode, "requestId", requestId,
        "orderId", command.refundCode(), "lang", "vi", "signature", hmacSha256(raw, momoSecretKey));
    JsonNode response = restClient.post().uri(momoRefundQueryEndpoint).contentType(MediaType.APPLICATION_JSON)
        .body(body).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("resultCode").asInt(-1);
    String message = response == null ? "MoMo không trả về dữ liệu" : response.path("message").asText();
    if (code == 0) {
      for (JsonNode item : response.path("refundTrans")) {
        if (command.refundCode().equals(item.path("orderId").asText())
            && item.path("resultCode").asInt(-1) == 0) {
          return new RefundResult(RefundStatus.COMPLETED,
              item.has("transId") ? item.path("transId").asText() : null, message);
        }
      }
      if (!response.has("refundTrans")) return new RefundResult(RefundStatus.COMPLETED, null, message);
    }
    return new RefundResult(code == 7002 || code == 1000 ? RefundStatus.PENDING : RefundStatus.FAILED,
        null, message);
  }

  private long amount(BigDecimal value) {
    return value.setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }

  private long parseLong(String value, String message) {
    try { return Long.parseLong(value); }
    catch (NumberFormatException exception) { throw new IllegalArgumentException(message); }
  }
}
