package com.ntt.language_center_management.filters;

import com.ntt.language_center_management.service.SystemLogService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.Principal;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class SystemLoggingFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(SystemLoggingFilter.class);
  public static final String REQUEST_ID_HEADER = "X-Request-ID";
  private final SystemLogService systemLogService;
  public SystemLoggingFilter(SystemLogService systemLogService) { this.systemLogService = systemLogService; }

  @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {
    String requestId = UUID.randomUUID().toString();
    response.setHeader(REQUEST_ID_HEADER, requestId);
    MDC.put("requestId", requestId);
    try { filterChain.doFilter(request, response); }
    finally {
      if (response.getStatus() >= 400 && !request.getRequestURI().equals("/api/admin/system-logs")) {
        Principal principal = request.getUserPrincipal();
        try {
          systemLogService.recordHttpFailure(response.getStatus(), request.getMethod(), request.getRequestURI(),
              requestId, principal == null ? null : principal.getName());
        } catch (RuntimeException exception) {
          // Logging must never replace or break the original API response.
          log.error("Could not persist system log: requestId={}, path={}",
              requestId, request.getRequestURI(), exception);
        }
      }
      MDC.remove("requestId");
    }
  }
}
