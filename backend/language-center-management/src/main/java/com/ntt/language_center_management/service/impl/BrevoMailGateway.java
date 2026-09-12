package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.exception.MailDeliveryException;
import com.ntt.language_center_management.service.MailGateway;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@ConditionalOnProperty(name = "app.mail.enabled", havingValue = "true")
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoMailGateway implements MailGateway {
  private static final int MAX_MESSAGE_VERSIONS_PER_REQUEST = 100;

  private final RestClient restClient;
  private final Map<String, String> sender;

  @Autowired
  public BrevoMailGateway(
      @Value("${app.mail.brevo.base-url:https://api.brevo.com}") String baseUrl,
      @Value("${app.mail.brevo.api-key:}") String apiKey,
      @Value("${app.mail.from:}") String from) {
    this(RestClient.builder(), baseUrl, apiKey, from);
  }

  public BrevoMailGateway(
      RestClient.Builder builder, String baseUrl, String apiKey, String from) {
    if (!StringUtils.hasText(apiKey)) {
      throw new IllegalStateException("BREVO_API_KEY is required when MAIL_PROVIDER=brevo");
    }
    this.sender = parseSender(from);
    this.restClient = builder
        .baseUrl(baseUrl)
        .defaultHeader("api-key", apiKey.trim())
        .defaultHeader("accept", MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Override
  public void send(String recipient, String subject, String body) {
    if (!StringUtils.hasText(recipient)) return;
    Map<String, Object> payload = basePayload(subject, body);
    payload.put("to", List.of(recipient(recipient)));
    execute(payload);
  }

  @Override
  public void sendBatch(List<String> recipients, String subject, String body) {
    List<String> validRecipients = recipients.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .toList();
    for (int start = 0; start < validRecipients.size();
        start += MAX_MESSAGE_VERSIONS_PER_REQUEST) {
      int end = Math.min(start + MAX_MESSAGE_VERSIONS_PER_REQUEST, validRecipients.size());
      Map<String, Object> payload = basePayload(subject, body);
      payload.put("messageVersions", validRecipients.subList(start, end).stream()
          .map(email -> Map.of("to", List.of(recipient(email))))
          .toList());
      execute(payload);
    }
  }

  private Map<String, Object> basePayload(String subject, String body) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("sender", sender);
    payload.put("subject", subject);
    payload.put("textContent", body);
    return payload;
  }

  private Map<String, String> recipient(String email) {
    return Map.of("email", email.trim());
  }

  private Map<String, String> parseSender(String from) {
    if (!StringUtils.hasText(from)) {
      throw new IllegalStateException("MAIL_FROM is required when MAIL_PROVIDER=brevo");
    }
    String value = from.trim();
    int openingBracket = value.lastIndexOf('<');
    int closingBracket = value.endsWith(">") ? value.length() - 1 : -1;
    String email = openingBracket >= 0 && closingBracket > openingBracket
        ? value.substring(openingBracket + 1, closingBracket).trim()
        : value;
    if (!email.contains("@")) {
      throw new IllegalStateException("MAIL_FROM must contain a valid sender email");
    }
    Map<String, String> parsed = new LinkedHashMap<>();
    parsed.put("email", email);
    if (openingBracket > 0) {
      String name = value.substring(0, openingBracket).trim();
      if (StringUtils.hasText(name)) parsed.put("name", name);
    }
    return parsed;
  }

  private void execute(Object payload) {
    try {
      restClient.post()
          .uri("/v3/smtp/email")
          .contentType(MediaType.APPLICATION_JSON)
          .body(payload)
          .retrieve()
          .toBodilessEntity();
    } catch (RestClientResponseException exception) {
      throw new MailDeliveryException(
          "Brevo rejected email request with HTTP " + exception.getStatusCode().value(),
          exception);
    } catch (ResourceAccessException exception) {
      throw new MailDeliveryException("Could not connect to Brevo API", exception);
    }
  }
}
