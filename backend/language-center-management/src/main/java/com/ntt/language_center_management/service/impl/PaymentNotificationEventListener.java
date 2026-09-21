package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.event.PaymentSucceededEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** In-app delivery remains enabled when email is disabled. */
@Component
public class PaymentNotificationEventListener {
  private static final Logger log = LoggerFactory.getLogger(PaymentNotificationEventListener.class);
  private final PaymentNotificationWriter writer;

  public PaymentNotificationEventListener(PaymentNotificationWriter writer) {
    this.writer = writer;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void paymentSucceeded(PaymentSucceededEvent event) {
    try {
      writer.write(event);
    } catch (RuntimeException exception) {
      // Payment is already committed; notification delivery must not request a payment retry.
      log.error("Cannot store notification for payment {}", event.transactionCode(), exception);
    }
  }
}
