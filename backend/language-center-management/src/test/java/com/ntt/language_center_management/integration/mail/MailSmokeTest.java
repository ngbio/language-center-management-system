package com.ntt.language_center_management.integration.mail;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

class MailSmokeTest {

  @Test
  void sendsOneRealEmailOnlyWhenExplicitlyEnabled() throws IOException {
    Assumptions.assumeTrue(Boolean.getBoolean("mail.smoke.enabled"),
        "Real email smoke test is disabled by default");

    String recipient = System.getProperty("mail.smoke.recipient", "").trim();
    Assumptions.assumeTrue(!recipient.isBlank(), "A smoke-test recipient is required");

    Properties environment = loadEnvironment();
    JavaMailSenderImpl sender = new JavaMailSenderImpl();
    sender.setHost(required(environment, "SPRING_MAIL_HOST"));
    sender.setPort(Integer.parseInt(required(environment, "SPRING_MAIL_PORT")));
    sender.setUsername(required(environment, "SPRING_MAIL_USERNAME"));
    sender.setPassword(required(environment, "SPRING_MAIL_PASSWORD"));

    Properties mailProperties = sender.getJavaMailProperties();
    mailProperties.put("mail.smtp.auth", environment.getProperty("SPRING_MAIL_SMTP_AUTH", "true"));
    mailProperties.put("mail.smtp.starttls.enable",
        environment.getProperty("SPRING_MAIL_STARTTLS_ENABLE", "true"));
    mailProperties.put("mail.smtp.starttls.required",
        environment.getProperty("SPRING_MAIL_STARTTLS_REQUIRED", "true"));
    mailProperties.put("mail.smtp.connectiontimeout", "10000");
    mailProperties.put("mail.smtp.timeout", "10000");
    mailProperties.put("mail.smtp.writetimeout", "10000");

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(required(environment, "MAIL_FROM"));
    message.setTo(recipient);
    message.setSubject("[Lingua Center] Kiểm tra gửi email");
    message.setText("Đây là email kiểm tra từ hệ thống quản lý trung tâm ngoại ngữ.\n\n"
        + "Nếu bạn nhận được thư này, cấu hình Spring Mail đang hoạt động bình thường.");

    assertDoesNotThrow(() -> sender.send(message));
  }

  private Properties loadEnvironment() throws IOException {
    Properties properties = new Properties();
    try (Reader reader = Files.newBufferedReader(Path.of(".env"), StandardCharsets.UTF_8)) {
      properties.load(reader);
    }
    return properties;
  }

  private String required(Properties properties, String key) {
    String value = properties.getProperty(key, "").trim();
    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
      value = value.substring(1, value.length() - 1);
    }
    if (value.isBlank()) throw new IllegalStateException(key + " is not configured");
    return value;
  }
}
