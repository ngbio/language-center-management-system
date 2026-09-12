package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.PasswordResetToken;
import jakarta.persistence.LockModeType;
import java.util.Date;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from PasswordResetToken token where token.tokenHash = :tokenHash")
  Optional<PasswordResetToken> lockByTokenHash(@Param("tokenHash") String tokenHash);

  Optional<PasswordResetToken> findByTokenHash(String tokenHash);

  boolean existsByUserId_IdAndCreatedAtAfterAndUsedAtIsNull(Integer userId, Date createdAfter);

  @Modifying
  @Query("update PasswordResetToken token set token.usedAt = :now "
      + "where token.userId.id = :userId and token.usedAt is null")
  int invalidateActiveTokens(@Param("userId") Integer userId, @Param("now") Date now);
}
