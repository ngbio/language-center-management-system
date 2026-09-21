package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.payment.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class GatewayRegistryTest {
  @Test
  void paymentRegistrySelectsByMethodAndRejectsMissingOrDuplicateProviders() {
    var momo = mock(PaymentGateway.class);
    var zalo = mock(PaymentGateway.class);
    when(momo.method()).thenReturn(PaymentMethod.MOMO);
    when(zalo.method()).thenReturn(PaymentMethod.ZALOPAY);
    var registry = new PaymentGatewayRegistry(List.of(zalo, momo));
    assertThat(registry.getRequired(PaymentMethod.MOMO)).isSameAs(momo);
    assertThat(registry.getRequired(PaymentMethod.ZALOPAY)).isSameAs(zalo);
    assertThatThrownBy(() -> registry.getRequired(null)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new PaymentGatewayRegistry(List.of(momo, momo))).hasMessageContaining("Trùng gateway");
    assertThatThrownBy(() -> new PaymentGatewayRegistry(List.of(momo)).getRequired(PaymentMethod.ZALOPAY))
        .hasMessageContaining("không được hỗ trợ");
  }

  @Test
  void refundRegistryRejectsUnsupportedRefundCapability() {
    var gateway = mock(RefundGateway.class);
    when(gateway.method()).thenReturn(PaymentMethod.MOMO);
    var registry = new RefundGatewayRegistry(List.of(gateway));
    assertThat(registry.getRequired(PaymentMethod.MOMO)).isSameAs(gateway);
    assertThatThrownBy(() -> registry.getRequired(PaymentMethod.ZALOPAY)).hasMessageContaining("không hỗ trợ hoàn tiền");
    assertThatThrownBy(() -> new RefundGatewayRegistry(List.of(gateway, gateway))).hasMessageContaining("Trùng gateway");
  }
}
