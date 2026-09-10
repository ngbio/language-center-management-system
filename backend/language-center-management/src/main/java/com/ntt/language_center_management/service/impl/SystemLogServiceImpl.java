package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.dto.response.SystemLogResponse;
import com.ntt.language_center_management.entity.SystemLog;
import com.ntt.language_center_management.enums.SystemLogLevel;
import com.ntt.language_center_management.repository.SystemLogRepository;
import com.ntt.language_center_management.service.SystemLogService;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SystemLogServiceImpl implements SystemLogService {
  private static final Logger log = LoggerFactory.getLogger(SystemLogServiceImpl.class);
  private final SystemLogRepository repository;
  public SystemLogServiceImpl(SystemLogRepository repository) { this.repository = repository; }

  @Override @Transactional(readOnly = true)
  public PageResponse<SystemLogResponse> search(String level, String eventType, String requestId,
      LocalDateTime from, LocalDateTime to, int page, int size) {
    if (page < 0) throw new IllegalArgumentException("Số trang không được nhỏ hơn 0");
    if (size < 1 || size > 100) throw new IllegalArgumentException("Kích thước trang phải từ 1 đến 100");
    if (from != null && to != null && from.isAfter(to))
      throw new IllegalArgumentException("Thời gian bắt đầu không được sau thời gian kết thúc");
    SystemLogLevel parsedLevel = null;
    if (StringUtils.hasText(level)) {
      try { parsedLevel = SystemLogLevel.valueOf(level.trim().toUpperCase(Locale.ROOT)); }
      catch (IllegalArgumentException exception) { throw new IllegalArgumentException("Mức log phải là WARN hoặc ERROR"); }
    }
    final SystemLogLevel selectedLevel = parsedLevel;
    final String selectedEvent = normalize(eventType);
    final String selectedRequestId = normalize(requestId);
    return PageResponse.from(repository.findAll((root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (selectedLevel != null) predicates.add(builder.equal(root.get("level"), selectedLevel));
      if (selectedEvent != null) predicates.add(builder.equal(builder.upper(root.get("eventType")), selectedEvent.toUpperCase(Locale.ROOT)));
      if (selectedRequestId != null) predicates.add(builder.equal(root.get("requestId"), selectedRequestId));
      if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("createdAt"), from));
      if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("createdAt"), to));
      return builder.and(predicates.toArray(Predicate[]::new));
    }, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))).map(this::toResponse));
  }

  @Override @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void recordHttpFailure(int status, String method, String path, String requestId, String actorEmail) {
    try {
      SystemLog entry = new SystemLog();
      entry.setLevel(status >= 500 ? SystemLogLevel.ERROR : SystemLogLevel.WARN);
      entry.setEventType(status == 401 || status == 403 ? "AUTHENTICATION_FAILURE" : "HTTP_REQUEST_FAILED");
      entry.setMessage(method + " " + path + " failed with HTTP " + status);
      entry.setRequestId(requestId); entry.setActorEmail(actorEmail); entry.setHttpMethod(method);
      entry.setRequestPath(path); entry.setHttpStatus(status); entry.setCreatedAt(LocalDateTime.now());
      repository.save(entry);
    } catch (RuntimeException exception) {
      log.error("Could not persist system log: requestId={}, path={}", requestId, path, exception);
    }
  }

  private SystemLogResponse toResponse(SystemLog entry) {
    return new SystemLogResponse(entry.getId(), entry.getLevel().name(), entry.getEventType(),
        entry.getMessage(), entry.getRequestId(), entry.getActorEmail(), entry.getHttpMethod(),
        entry.getRequestPath(), entry.getHttpStatus(), entry.getCreatedAt());
  }
  private String normalize(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
