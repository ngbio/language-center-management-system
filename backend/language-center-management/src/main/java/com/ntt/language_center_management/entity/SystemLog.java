package com.ntt.language_center_management.entity;

import com.ntt.language_center_management.enums.SystemLogLevel;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
public class SystemLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, length = 10) @Enumerated(EnumType.STRING) private SystemLogLevel level;
  @Column(name = "event_type", nullable = false, length = 50) private String eventType;
  @Column(nullable = false, length = 500) private String message;
  @Column(name = "request_id", length = 36) private String requestId;
  @Column(name = "actor_email", length = 150) private String actorEmail;
  @Column(name = "http_method", length = 10) private String httpMethod;
  @Column(name = "request_path", length = 255) private String requestPath;
  @Column(name = "http_status") private Integer httpStatus;
  @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public SystemLogLevel getLevel() { return level; }
  public void setLevel(SystemLogLevel level) { this.level = level; }
  public String getEventType() { return eventType; }
  public void setEventType(String eventType) { this.eventType = eventType; }
  public String getMessage() { return message; }
  public void setMessage(String message) { this.message = message; }
  public String getRequestId() { return requestId; }
  public void setRequestId(String requestId) { this.requestId = requestId; }
  public String getActorEmail() { return actorEmail; }
  public void setActorEmail(String actorEmail) { this.actorEmail = actorEmail; }
  public String getHttpMethod() { return httpMethod; }
  public void setHttpMethod(String httpMethod) { this.httpMethod = httpMethod; }
  public String getRequestPath() { return requestPath; }
  public void setRequestPath(String requestPath) { this.requestPath = requestPath; }
  public Integer getHttpStatus() { return httpStatus; }
  public void setHttpStatus(Integer httpStatus) { this.httpStatus = httpStatus; }
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
