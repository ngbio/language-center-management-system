package com.ntt.language_center_management.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.StringUtils;

public final class PaymentGatewayUtils {

  private PaymentGatewayUtils() {}

  public static void requireConfig(String value, String name) {
    if (!StringUtils.hasText(value) || value.startsWith("CHANGE_ME")) {
      throw new IllegalArgumentException("Thiếu cấu hình " + name);
    }
  }

  public static String hmacSha256(String value, String key) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception exception) {
      throw new IllegalStateException("Không thể tạo chữ ký cổng thanh toán", exception);
    }
  }

  public static boolean constantEquals(String left, String right) {
    return left != null
        && right != null
        && MessageDigest.isEqual(
            left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
  }

  public static String stringValue(Object value) {
    return value == null ? "" : String.valueOf(value);
  }
}
