package com.ntt.language_center_management.unit.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.exception.ChatUnavailableException;
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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

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
    assertError(
        handler.handleChatUnavailable(new ChatUnavailableException("chat unavailable")),
        HttpStatus.SERVICE_UNAVAILABLE,
        "chat unavailable");
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

  @Test
  void shouldJoinDistinctValidationErrorsWithFieldNames() {
    MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
    BindingResult bindingResult = mock(BindingResult.class);
    when(exception.getBindingResult()).thenReturn(bindingResult);
    when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
        new FieldError("request", "email", "không hợp lệ"),
        new FieldError("request", "password", "không được để trống"),
        new FieldError("request", "email", "không hợp lệ")));

    var response = handler.handleValidation(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().message())
        .isEqualTo("email: không hợp lệ; password: không được để trống");
  }

  @Test
  void shouldReturnBadRequestForMissingPartParameterAndTypeMismatch() {
    var missingPart = handler.handleMalformedRequest(mock(MissingServletRequestPartException.class));
    var missingParameter =
        handler.handleMalformedRequest(mock(MissingServletRequestParameterException.class));
    var typeMismatch =
        handler.handleMalformedRequest(mock(MethodArgumentTypeMismatchException.class));

    assertThat(missingPart.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(missingParameter.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(typeMismatch.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(missingPart.getBody().message()).isEqualTo(missingParameter.getBody().message());
    assertThat(missingPart.getBody().message()).isEqualTo(typeMismatch.getBody().message());
  }

  private void assertError(
      ResponseEntity<ApiResponse<Void>> response, HttpStatus status, String message) {
    assertThat(response.getStatusCode()).isEqualTo(status);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().status()).isEqualTo(status.value());
    assertThat(response.getBody().message()).isEqualTo(message);
  }
}
