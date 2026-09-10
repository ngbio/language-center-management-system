package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.dto.response.SystemLogResponse;
import java.time.LocalDateTime;

public interface SystemLogService {
  PageResponse<SystemLogResponse> search(String level, String eventType, String requestId,
      LocalDateTime from, LocalDateTime to, int page, int size);
  void recordHttpFailure(int status, String method, String path, String requestId, String actorEmail);
}
