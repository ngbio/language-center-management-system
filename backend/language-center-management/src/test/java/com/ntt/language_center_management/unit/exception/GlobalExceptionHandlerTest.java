package com.ntt.language_center_management.unit.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.GlobalExceptionHandler;
import com.ntt.language_center_management.exception.PaymentGatewayException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void shouldMapExpectedStatusWhenDomainExceptionOccurs() {
    assertError(
        handler.handleDuplicateResource(new DuplicateResourceException("duplicate")),
        HttpStatus.CONFLICT,
        "duplicate");
    assertError(
        handler.handleResourceNotFound(new ResourceNotFoundException("missing")),
        HttpStatus.NOT_FOUND,
        "missing");
    assertError(
        handler.handleUnauthorized(new UnauthorizedException("login")),
        HttpStatus.UNAUTHORIZED,
        "login");
    assertError(
        handler.handleForbidden(new ForbiddenException("denied")),
        HttpStatus.FORBIDDEN,
        "denied");
    assertError(
        handler.handleBadRequest(new IllegalArgumentException("invalid")),
        HttpStatus.BAD_REQUEST,
        "invalid");
    assertError(
        handler.handlePaymentGateway(new PaymentGatewayException("gateway")),
        HttpStatus.BAD_GATEWAY,
        "gateway");
  }

  @Test
  void shouldReturnSafeBadRequestWhenJsonIsMalformed() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleUnreadableRequest(mock(HttpMessageNotReadableException.class));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).doesNotContain("secret parser error");
  }

  @Test
  void shouldReturnPayloadTooLargeWhenUploadExceedsLimit() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleUploadTooLarge(new MaxUploadSizeExceededException(1024));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(413);
  }

  @Test
  void shouldReturnGenericServerErrorWhenExceptionIsUnexpected() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleUnexpected(new RuntimeException("database password must stay private"));

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message()).doesNotContain("database password");
  }

  private void assertError(
      ResponseEntity<ApiResponse<Void>> response, HttpStatus status, String message) {
    assertThat(response.getStatusCode()).isEqualTo(status);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(status.value());
    assertThat(response.getBody().message()).isEqualTo(message);
  }
}
