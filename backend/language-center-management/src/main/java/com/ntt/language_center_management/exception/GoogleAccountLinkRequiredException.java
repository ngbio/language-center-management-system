package com.ntt.language_center_management.exception;

public class GoogleAccountLinkRequiredException extends RuntimeException {
  public GoogleAccountLinkRequiredException() {
    super("Email đã có tài khoản. Nhập mật khẩu hiện tại để xác nhận liên kết với Google.");
  }
}
