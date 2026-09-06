package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Refund;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefundRepository extends JpaRepository<Refund, Integer> {
  Optional<Refund> findByIdempotencyKey(String idempotencyKey);
  List<Refund> findByEnrollment_IdOrderByCreatedAtDesc(Integer enrollmentId);
  List<Refund> findByEnrollment_StudentId_IdOrderByCreatedAtDesc(Integer studentId);
}
