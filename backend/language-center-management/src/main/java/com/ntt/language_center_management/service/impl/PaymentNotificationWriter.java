package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.event.PaymentSucceededEvent;
import com.ntt.language_center_management.repository.NotificationRepository;
import java.time.Clock;
import java.util.Date;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentNotificationWriter {
  private final NotificationRepository notifications;
  private final Clock clock;

  public PaymentNotificationWriter(NotificationRepository notifications, Clock clock) {
    this.notifications = notifications;
    this.clock = clock;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void write(PaymentSucceededEvent event) {
    notifications.insertPaymentNotification(event.userId(), "Thanh toán học phí thành công",
        "Đã ghi nhận thanh toán " + event.amount().toPlainString() + " VND cho lớp "
            + event.className() + ". Mã giao dịch: " + event.transactionCode() + ".",
        Date.from(clock.instant()), "PAYMENT_SUCCESS:" + event.transactionCode() + ":" + event.userId());
  }
}
