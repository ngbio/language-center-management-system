package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.event.AccountCreatedMailEvent;
import com.ntt.language_center_management.event.ClassOpenedMailEvent;
import com.ntt.language_center_management.event.PaymentSucceededMailEvent;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.impl.MailNotificationEventListener;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class MailNotificationEventListenerTest {
  private JavaMailSender sender;
  private UserRepository users;
  private MailNotificationEventListener listener;

  @BeforeEach
  void setUp() {
    sender = mock(JavaMailSender.class);
    users = mock(UserRepository.class);
    listener = new MailNotificationEventListener(
        sender, users, "no-reply@lingua.test", 2, "Asia/Ho_Chi_Minh");
  }

  @Test
  void shouldSendWelcomeAndPaymentEmails() {
    listener.accountCreated(new AccountCreatedMailEvent("student@example.com", "Nguyễn An"));
    listener.paymentSucceeded(new PaymentSucceededMailEvent(
        "student@example.com", "Nguyễn An", "TX-01", "English A1",
        new BigDecimal("3200000")));

    ArgumentCaptor<SimpleMailMessage> messages =
        ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender, times(2)).send(messages.capture());
    assertThat(messages.getAllValues().get(0).getSubject()).contains("Chào mừng");
    assertThat(messages.getAllValues().get(1).getText()).contains("TX-01", "English A1");
  }

  @Test
  void shouldPageThroughStudentsInsteadOfLoadingAllRecipients() {
    Date startDate = Date.from(LocalDate.of(2026, 10, 1).atStartOfDay(
        ZoneId.of("Asia/Ho_Chi_Minh")).toInstant());
    when(users.findActiveStudentEmails(PageRequest.of(0, 2)))
        .thenReturn(new PageImpl<>(List.of("one@example.com", "two@example.com"),
            PageRequest.of(0, 2), 3));
    when(users.findActiveStudentEmails(PageRequest.of(1, 2)))
        .thenReturn(new PageImpl<>(List.of("three@example.com"),
            PageRequest.of(1, 2), 3));

    listener.classOpened(new ClassOpenedMailEvent(
        "EN-A1-01", "English A1", startDate, new BigDecimal("3200000")));

    verify(users).findActiveStudentEmails(PageRequest.of(0, 2));
    verify(users).findActiveStudentEmails(PageRequest.of(1, 2));
    verify(sender, times(2)).send(any(SimpleMailMessage[].class));
  }
}
