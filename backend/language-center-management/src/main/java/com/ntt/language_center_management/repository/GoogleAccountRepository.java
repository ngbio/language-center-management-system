package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.GoogleAccount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoogleAccountRepository extends JpaRepository<GoogleAccount, Integer> {
  Optional<GoogleAccount> findByFirebaseUid(String firebaseUid);
  boolean existsByUser_Id(Integer userId);
}
