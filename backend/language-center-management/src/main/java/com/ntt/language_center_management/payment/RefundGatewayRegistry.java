package com.ntt.language_center_management.payment;

import com.ntt.language_center_management.enums.PaymentMethod;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RefundGatewayRegistry {
  private final Map<PaymentMethod, RefundGateway> gateways;

  public RefundGatewayRegistry(List<RefundGateway> implementations) {
    var registered = new EnumMap<PaymentMethod, RefundGateway>(PaymentMethod.class);
    for (var gateway : implementations) {
      if (registered.putIfAbsent(gateway.method(), gateway) != null) {
        throw new IllegalArgumentException("Trùng gateway: " + gateway.method());
      }
    }
    gateways = Map.copyOf(registered);
  }

  public RefundGateway getRequired(PaymentMethod method) {
    var gateway = method == null ? null : gateways.get(method);
    if (gateway == null) throw new IllegalArgumentException("Phương thức thanh toán không hỗ trợ hoàn tiền");
    return gateway;
  }
}
