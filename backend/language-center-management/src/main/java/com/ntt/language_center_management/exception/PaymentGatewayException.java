package com.ntt.language_center_management.exception;

public class PaymentGatewayException extends RuntimeException {
  public PaymentGatewayException(String message) {
    super(message);
  }

  public PaymentGatewayException(String message, Throwable cause) {
    super(message, cause);
  }
}
