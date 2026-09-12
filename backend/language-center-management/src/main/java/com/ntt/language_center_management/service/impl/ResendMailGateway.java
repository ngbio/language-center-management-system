package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.exception.MailDeliveryException;
import com.ntt.language_center_management.service.MailGateway;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "resend")
public class ResendMailGateway implements MailGateway {
  private static final int MAX_BATCH_SIZE = 100;

  private final RestClient restClient;
  private final String from;

  public ResendMailGateway(
      RestClient.Builder builder,
      @Value("${app.mail.resend.base-url:https://api.resend.com}") String baseUrl,
      @Value("${app.mail.resend.api-key:}") String apiKey,
      @Value("${app.mail.from:}") String from) {
    if (!StringUtils.hasText(apiKey)) {
      throw new IllegalStateException("RESEND_API_KEY is required when MAIL_PROVIDER=resend");
    }
    if (!StringUtils.hasText(from)) {
      throw new IllegalStateException("MAIL_FROM is required when MAIL_PROVIDER=resend");
    }
    this.from = from.trim();
    this.restClient = builder
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey.trim())
        .defaultHeader(HttpHeaders.USER_AGENT, "language-center-management/1.0")
        .build();
  }

  @Override
  public void send(String recipient, String subject, String body) {
    if (!StringUtils.hasText(recipient)) return;
    execute("/emails", payload(recipient, subject, body));
  }

  @Override
  public void sendBatch(List<String> recipients, String subject, String body) {
    List<String> validRecipients = recipients.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
    for (int start = 0; start < validRecipients.size(); start += MAX_BATCH_SIZE) {
      int end = Math.min(start + MAX_BATCH_SIZE, validRecipients.size());
      List<Map<String, Object>> batch = validRecipients.subList(start, end).stream()
          .map(recipient -> payload(recipient, subject, body))
          .toList();
      execute("/emails/batch", batch);
    }
  }

  private Map<String, Object> payload(String recipient, String subject, String body) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("from", from);
    payload.put("to", List.of(recipient.trim()));
    payload.put("subject", subject);
    payload.put("text", body);
    return payload;
  }

  private void execute(String path, Object payload) {
    try {
      restClient.post()
          .uri(path)
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      throw new MailDeliveryException(
          "Resend rejected email request with HTTP " + exception.getStatusCode().value(),
          exception);
    } catch (ResourceAccessException exception) {
      throw new MailDeliveryException("Could not connect to Resend API", exception);
    }
  }
}
