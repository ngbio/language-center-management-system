package com.ntt.language_center_management.integration.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.event.PaymentSucceededEvent;
import com.ntt.language_center_management.repository.NotificationRepository;
import com.ntt.language_center_management.service.impl.PaymentNotificationEventListener;
import com.ntt.language_center_management.service.impl.PaymentNotificationWriter;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

/** Exercises real Spring event/transaction proxies with mocked JDBC and repository, not a live DB. */
class PaymentNotificationTransactionTest {
  private static final Instant NOW = Instant.parse("2026-09-21T08:00:00Z");

  @Configuration
  @EnableTransactionManagement
  @Import({PaymentNotificationWriter.class, PaymentNotificationEventListener.class})
  static class Config {
    @Bean Clock clock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    @Bean NotificationRepository notifications() { return mock(NotificationRepository.class); }
    @Bean DataSource dataSource() throws Exception {
      var source = mock(DataSource.class);
      when(source.getConnection()).thenAnswer(invocation -> {
        var connection = mock(Connection.class);
        when(connection.getAutoCommit()).thenReturn(true);
        return connection;
      });
      return source;
    }
    @Bean PlatformTransactionManager transactionManager(DataSource source) {
      return new DataSourceTransactionManager(source);
    }
  }

  @Test
  void writesOnlyAfterCommitUsingANewTransactionEvenWithoutMailConfiguration() throws Exception {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var notifications = context.getBean(NotificationRepository.class);
      var source = context.getBean(DataSource.class);
      var transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
      transaction.executeWithoutResult(status -> {
        context.publishEvent(event());
        verifyNoInteractions(notifications);
      });
      verify(notifications).insertPaymentNotification(eq(9), eq("Thanh toán học phí thành công"),
          contains("TX-123"), eq(Date.from(NOW)), eq("PAYMENT_SUCCESS:TX-123:9"));
      verify(source, times(2)).getConnection();
    }
  }

  @Test
  void rollbackDoesNotDeliverNotification() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      new TransactionTemplate(context.getBean(PlatformTransactionManager.class)).executeWithoutResult(status -> {
        context.publishEvent(event());
        status.setRollbackOnly();
      });
      verifyNoInteractions(context.getBean(NotificationRepository.class));
    }
  }

  @Test
  void notificationFailureDoesNotFailAnAlreadyCommittedPayment() {
    try (var context = new AnnotationConfigApplicationContext(Config.class)) {
      var notifications = context.getBean(NotificationRepository.class);
      when(notifications.insertPaymentNotification(anyInt(), anyString(), anyString(), any(), anyString()))
          .thenThrow(new IllegalStateException("simulated notification outage"));
      assertThatCode(() -> new TransactionTemplate(context.getBean(PlatformTransactionManager.class))
          .executeWithoutResult(status -> context.publishEvent(event()))).doesNotThrowAnyException();
      verify(notifications).insertPaymentNotification(anyInt(), anyString(), anyString(), any(), anyString());
    }
  }

  private PaymentSucceededEvent event() {
    return new PaymentSucceededEvent(9, "student@example.com", "Học viên", "TX-123", "Anh văn A1", new BigDecimal("3200000"));
  }
}
