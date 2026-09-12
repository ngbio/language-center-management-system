package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ntt.language_center_management.service.impl.BrevoMailGateway;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoMailGatewayTest {

  @Test
  void sendsTransactionalEmailThroughBrevoHttpsApi() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    BrevoMailGateway gateway = new BrevoMailGateway(
        builder, "https://api.brevo.test", "xkeysib-test", "Lingua <noreply@example.com>");

    server.expect(requestTo("https://api.brevo.test/v3/smtp/email"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("api-key", "xkeysib-test"))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          assertThat(body).contains(
              "\"sender\":{\"email\":\"noreply@example.com\",\"name\":\"Lingua\"}",
              "\"to\":[{\"email\":\"student@example.com\"}]",
              "\"subject\":\"Chào mừng\"",
              "\"textContent\":\"Nội dung email\"");
        })
        .andRespond(withSuccess("{\"messageId\":\"message-1\"}",
            MediaType.APPLICATION_JSON));

    gateway.send("student@example.com", "Chào mừng", "Nội dung email");
    server.verify();
  }

  @Test
  void sendsAnnouncementsPrivatelyWithMessageVersions() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    BrevoMailGateway gateway = new BrevoMailGateway(
        builder, "https://api.brevo.test", "xkeysib-test", "noreply@example.com");

    server.expect(requestTo("https://api.brevo.test/v3/smtp/email"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          assertThat(body).contains(
              "\"messageVersions\"", "one@example.com", "two@example.com");
        })
        .andRespond(withSuccess("{\"messageIds\":[\"1\",\"2\"]}",
            MediaType.APPLICATION_JSON));

    gateway.sendBatch(List.of("one@example.com", "two@example.com", "one@example.com", " "),
        "Lớp mới", "Nội dung");
    server.verify();
  }

  @Test
  void refusesToStartWithoutRequiredConfiguration() {
    assertThatThrownBy(() -> new BrevoMailGateway(
        RestClient.builder(), "https://api.brevo.com", "", "noreply@example.com"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("BREVO_API_KEY");
    assertThatThrownBy(() -> new BrevoMailGateway(
        RestClient.builder(), "https://api.brevo.com", "xkeysib-test", ""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MAIL_FROM");
  }

  @Test
  void productionConstructorDoesNotRequireRestClientBuilderBean() {
    assertThat(new BrevoMailGateway(
        "https://api.brevo.com", "xkeysib-test", "noreply@example.com")).isNotNull();
  }
}
