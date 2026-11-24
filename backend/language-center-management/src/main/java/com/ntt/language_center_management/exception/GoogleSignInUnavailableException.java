package com.ntt.language_center_management.exception;

public class GoogleSignInUnavailableException extends RuntimeException {
  public GoogleSignInUnavailableException() {
    super("Đăng nhập Google chưa được cấu hình trên máy chủ. Vui lòng đăng nhập bằng email và mật khẩu.");
  }
}
