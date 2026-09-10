package com.ntt.language_center_management.controller.admin;

import com.ntt.language_center_management.dto.response.*;
import com.ntt.language_center_management.service.SystemLogService;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/system-logs")
public class SystemLogApiController {
  private final SystemLogService systemLogService;
  public SystemLogApiController(SystemLogService systemLogService) { this.systemLogService = systemLogService; }

  @GetMapping
  public ApiResponse<PageResponse<SystemLogResponse>> list(
      @RequestParam(required = false) String level, @RequestParam(required = false) String eventType,
      @RequestParam(required = false) String requestId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return new ApiResponse<>(HttpStatus.OK.value(), "Lấy nhật ký hệ thống thành công",
        systemLogService.search(level, eventType, requestId, from, to, page, size));
  }
}
