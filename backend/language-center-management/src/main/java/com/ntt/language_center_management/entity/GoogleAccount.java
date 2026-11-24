package com.ntt.language_center_management.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** A verified Firebase Google identity linked to one local account. */
@Entity
@Table(name = "google_account")
public class GoogleAccount {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "firebase_uid", nullable = false, unique = true, length = 128)
  private String firebaseUid;

  @OneToOne(optional = false)
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  private User user;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected GoogleAccount() {}

  public GoogleAccount(String firebaseUid, User user) {
    this.firebaseUid = firebaseUid;
    this.user = user;
    this.createdAt = Instant.now();
  }

  public String getFirebaseUid() { return firebaseUid; }
  public User getUser() { return user; }
}
