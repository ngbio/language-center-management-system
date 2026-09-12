package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ntt.language_center_management.service.impl.ResendMailGateway;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ResendMailGatewayTest {

  @Test
  void sendsTransactionalEmailThroughResendHttpsApi() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    ResendMailGateway gateway = new ResendMailGateway(
        builder, "https://api.resend.test", "re_test_secret", "Lingua <noreply@example.com>");

    server.expect(requestTo("https://api.resend.test/emails"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer re_test_secret"))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          org.assertj.core.api.Assertions.assertThat(body)
              .contains("\"from\":\"Lingua <noreply@example.com>\"",
                  "\"to\":[\"student@example.com\"]",
                  "\"subject\":\"Chào mừng\"",
                  "\"text\":\"Nội dung email\"");
        })
        .andRespond(withSuccess("{\"id\":\"email-1\"}", MediaType.APPLICATION_JSON));

    gateway.send("student@example.com", "Chào mừng", "Nội dung email");

    server.verify();
  }

  @Test
  void sendsClassAnnouncementAsResendBatch() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    ResendMailGateway gateway = new ResendMailGateway(
        builder, "https://api.resend.test", "re_test_secret", "noreply@example.com");

    server.expect(requestTo("https://api.resend.test/emails/batch"))
        .andExpect(method(HttpMethod.POST))
        .andExpect(request -> {
          String body = ((MockClientHttpRequest) request).getBodyAsString();
          org.assertj.core.api.Assertions.assertThat(body)
              .contains("one@example.com", "two@example.com", "Lớp mới")
              .doesNotContain("blank@example.com");
        })
        .andRespond(withSuccess("{\"data\":[{\"id\":\"1\"},{\"id\":\"2\"}]}",
            MediaType.APPLICATION_JSON));

    gateway.sendBatch(List.of("one@example.com", "two@example.com", "one@example.com", " "),
        "Lớp mới", "Nội dung");

    server.verify();
  }

  @Test
  void refusesToStartWithoutRequiredRailwaySecrets() {
    assertThatThrownBy(() -> new ResendMailGateway(
        RestClient.builder(), "https://api.resend.com", "", "noreply@example.com"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("RESEND_API_KEY");
    assertThatThrownBy(() -> new ResendMailGateway(
        RestClient.builder(), "https://api.resend.com", "re_test", ""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MAIL_FROM");
  }

  @Test
  void productionConstructorDoesNotRequireRestClientBuilderBean() {
    ResendMailGateway gateway = new ResendMailGateway(
        "https://api.resend.com", "re_test", "noreply@example.com");

    org.assertj.core.api.Assertions.assertThat(gateway).isNotNull();
  }
}
