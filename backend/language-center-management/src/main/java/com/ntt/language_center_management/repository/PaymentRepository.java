package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Payment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
  Optional<Payment> findByTransactionCodeAndMethod(String transactionCode, String method);
  Optional<Payment> findByTransactionCode(String transactionCode);
  List<Payment> findByEnrollmentId_StudentId_IdOrderByCreatedAtDesc(Integer studentId);
  List<Payment> findByEnrollmentId_IdOrderByCreatedAtDesc(Integer enrollmentId);
  List<Payment> findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(Integer enrollmentId, String status);
}
