package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.exception.PaymentGatewayException;
import java.net.URI;
import java.util.List;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientResponseException;
import static com.ntt.language_center_management.util.PaymentGatewayUtils.requireConfig;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class GatewaySupport {
  private GatewaySupport() {}
  static void requirePublicCallback(String value, String name) {
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

  static PaymentGatewayException gatewayRejected(
      ObjectMapper objectMapper, String gateway, RestClientResponseException exception) {
    String detail = gatewayMessage(objectMapper, exception.getResponseBodyAsString());
    String message = gateway + " sandbox từ chối yêu cầu (HTTP "
        + exception.getStatusCode().value() + ")";
    if (StringUtils.hasText(detail)) message += ": " + detail;
    return new PaymentGatewayException(message, exception);
  }

  private static String gatewayMessage(ObjectMapper objectMapper, String responseBody) {
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
