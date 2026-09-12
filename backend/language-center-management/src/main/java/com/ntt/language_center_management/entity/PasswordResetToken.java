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
import java.util.Date;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User userId;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "expires_at", nullable = false)
  private Date expiresAt;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "used_at")
  private Date usedAt;

  @Temporal(TemporalType.TIMESTAMP)
  @Column(name = "created_at", nullable = false)
  private Date createdAt;

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  public User getUserId() { return userId; }
  public void setUserId(User userId) { this.userId = userId; }
  public String getTokenHash() { return tokenHash; }
  public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
  public Date getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }
  public Date getUsedAt() { return usedAt; }
  public void setUsedAt(Date usedAt) { this.usedAt = usedAt; }
  public Date getCreatedAt() { return createdAt; }
  public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
