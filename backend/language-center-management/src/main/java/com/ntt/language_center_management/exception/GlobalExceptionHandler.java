package com.ntt.language_center_management.exception;

import com.ntt.language_center_management.dto.response.ApiResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ntt.language_center_management.controller")
public class GlobalExceptionHandler {

  @ExceptionHandler(DuplicateResourceException.class)
  public ResponseEntity<ApiResponse<Void>> handleDuplicateResource(
      DuplicateResourceException exception) {
    return error(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(
      ResourceNotFoundException exception) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException exception) {
    return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ApiResponse<Void>> handleForbidden(ForbiddenException exception) {
    return error(HttpStatus.FORBIDDEN, exception.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidation(
      MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .distinct()
            .collect(Collectors.joining("; "));
    return error(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  private ResponseEntity<ApiResponse<Void>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status).body(new ApiResponse<>(status.value(), message, null));
  }
}
