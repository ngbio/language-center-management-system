package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.enums.RefundStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.hmacSha256;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.requireConfig;
import tools.jackson.databind.JsonNode;

@Service
public class ZaloPayRefundGateway implements RefundGateway {
  private final RestClient restClient;
  @Value("${payment.zalopay.refund-endpoint}") private String zaloPayRefundEndpoint;
  @Value("${payment.zalopay.refund-query-endpoint}") private String zaloPayRefundQueryEndpoint;
  @Value("${payment.zalopay.app-id}") private String zaloPayAppId;
  @Value("${payment.zalopay.key1}") private String zaloPayKey1;

  @Autowired
  public ZaloPayRefundGateway() { this(RestClient.builder().build()); }

  public ZaloPayRefundGateway(RestClient restClient) { this.restClient = restClient; }

  @Override public PaymentMethod method() { return PaymentMethod.ZALOPAY; }

  @Override
  public void validate(String referenceCode) {
    if (!StringUtils.hasText(referenceCode)) {
      throw new IllegalArgumentException("Payment chưa có mã giao dịch từ cổng thanh toán");
    }
    requireConfig(zaloPayAppId, "ZALOPAY_APP_ID");
    requireConfig(zaloPayKey1, "ZALOPAY_KEY1");
    parseLong(referenceCode, "Mã giao dịch ZaloPay không hợp lệ");
  }

  @Override
  public String createRefundCode(Integer enrollmentId) {
    String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    return java.time.LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"))
        .format(java.time.format.DateTimeFormatter.ofPattern("yyMMdd"))
        + "_" + zaloPayAppId + "_RF" + enrollmentId + suffix;

  }

  @Override
  public RefundResult submit(RefundCommand command) {
    long timestamp = System.currentTimeMillis();
    long amount = amount(command.amount());
    String transId = command.referenceCode();
    String description = command.reason();
    String macInput = zaloPayAppId + "|" + transId + "|" + amount + "|" + description + "|" + timestamp;
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("app_id", zaloPayAppId);
    form.add("m_refund_id", command.refundCode());
    form.add("zp_trans_id", transId);
    form.add("amount", String.valueOf(amount));
    form.add("timestamp", String.valueOf(timestamp));
    form.add("description", description);
    form.add("mac", hmacSha256(macInput, zaloPayKey1));
    JsonNode response = restClient.post().uri(zaloPayRefundEndpoint)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("return_code").asInt(-1);
    String message = response == null ? "ZaloPay không trả về dữ liệu" : response.path("return_message").asText();
    return new RefundResult(code == 1 ? RefundStatus.COMPLETED
        : code == 3 ? RefundStatus.PENDING : RefundStatus.FAILED,
        response != null && response.has("refund_id") ? response.path("refund_id").asText() : null, message);
  }

  @Override
  public RefundResult query(RefundCommand command) {
    long timestamp = System.currentTimeMillis();
    String macInput = zaloPayAppId + "|" + command.refundCode() + "|" + timestamp;
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("app_id", zaloPayAppId);
    form.add("m_refund_id", command.refundCode());
    form.add("timestamp", String.valueOf(timestamp));
    form.add("mac", hmacSha256(macInput, zaloPayKey1));
    JsonNode response = restClient.post().uri(zaloPayRefundQueryEndpoint)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
    int code = response == null ? -1 : response.path("return_code").asInt(-1);
    String message = response == null ? "ZaloPay không trả về dữ liệu" : response.path("return_message").asText();
    return new RefundResult(code == 1 ? RefundStatus.COMPLETED
        : code == 3 ? RefundStatus.PENDING : RefundStatus.FAILED,
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
