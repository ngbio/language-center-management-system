package com.ntt.language_center_management.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "refund")
public class Refund {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  @ManyToOne(optional = false) @JoinColumn(name = "enrollment_id")
  private Enrollment enrollment;
  @ManyToOne(optional = false) @JoinColumn(name = "payment_id")
  private Payment payment;
  @ManyToOne(optional = false) @JoinColumn(name = "processed_by")
  private User processedBy;
  @Column(name = "refund_code", nullable = false, unique = true, length = 100)
  private String refundCode;
  @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
  private String idempotencyKey;
  @Column(nullable = false, precision = 18, scale = 2)
  private BigDecimal amount;
  @Column(nullable = false, length = 20)
  private String status;
  @Column(nullable = false, length = 500)
  private String reason;
  @Column(name = "created_at", nullable = false) @Temporal(TemporalType.TIMESTAMP)
  private Date createdAt;
  @Column(name = "completed_at") @Temporal(TemporalType.TIMESTAMP)
  private Date completedAt;

  public Integer getId() { return id; }
  public Enrollment getEnrollment() { return enrollment; }
  public void setEnrollment(Enrollment enrollment) { this.enrollment = enrollment; }
  public Payment getPayment() { return payment; }
  public void setPayment(Payment payment) { this.payment = payment; }
  public User getProcessedBy() { return processedBy; }
  public void setProcessedBy(User processedBy) { this.processedBy = processedBy; }
  public String getRefundCode() { return refundCode; }
  public void setRefundCode(String refundCode) { this.refundCode = refundCode; }
  public String getIdempotencyKey() { return idempotencyKey; }
  public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Date getCreatedAt() { return createdAt; }
  public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
  public Date getCompletedAt() { return completedAt; }
  public void setCompletedAt(Date completedAt) { this.completedAt = completedAt; }
}
