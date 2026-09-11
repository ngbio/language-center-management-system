package com.ntt.language_center_management.enums;

public enum SystemLogLevel {
  WARN, // Yêu cầu bị từ chối hoặc lỗi phía client/nghiệp vụ (HTTP 4xx).
  ERROR // Lỗi xử lý phía server (HTTP 5xx).
}
