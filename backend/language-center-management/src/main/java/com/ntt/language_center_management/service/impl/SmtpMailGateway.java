package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.exception.MailDeliveryException;
import com.ntt.language_center_management.service.MailGateway;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "smtp", matchIfMissing = true)
public class SmtpMailGateway implements MailGateway {

  private final JavaMailSender mailSender;
  private final String from;

  public SmtpMailGateway(
      JavaMailSender mailSender,
      @Value("${app.mail.from:${spring.mail.username:}}") String from) {
    this.mailSender = mailSender;
    this.from = from;
  }

  @Override
  public void send(String recipient, String subject, String body) {
    try {
      mailSender.send(message(recipient, subject, body));
    } catch (MailException exception) {
      throw new MailDeliveryException("SMTP could not deliver email", exception);
    }
  }

  @Override
  public void sendBatch(List<String> recipients, String subject, String body) {
    SimpleMailMessage[] messages = recipients.stream()
        .filter(StringUtils::hasText)
        .map(recipient -> message(recipient, subject, body))
        .toArray(SimpleMailMessage[]::new);
    if (messages.length == 0) return;
    try {
      mailSender.send(messages);
    } catch (MailException exception) {
      throw new MailDeliveryException("SMTP could not deliver email batch", exception);
    }
  }

  private SimpleMailMessage message(String recipient, String subject, String body) {
    SimpleMailMessage message = new SimpleMailMessage();
    if (StringUtils.hasText(from)) message.setFrom(from);
    message.setTo(recipient);
    message.setSubject(subject);
    message.setText(body);
    return message;
  }
}
