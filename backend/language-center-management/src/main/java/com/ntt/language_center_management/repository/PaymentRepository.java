package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.enums.PaymentMethod;
import com.ntt.language_center_management.enums.PaymentTransactionStatus;

import com.ntt.language_center_management.entity.Payment;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
  Optional<Payment> findByTransactionCodeAndMethod(
      String transactionCode, PaymentMethod method);
  Optional<Payment> findByTransactionCode(String transactionCode);
  List<Payment> findByEnrollmentId_StudentId_IdOrderByCreatedAtDesc(Integer studentId);
  List<Payment> findByEnrollmentId_IdOrderByCreatedAtDesc(Integer enrollmentId);
  List<Payment> findByEnrollmentId_IdAndStatusOrderByCompletedAtDesc(Integer enrollmentId,
      PaymentTransactionStatus status);

  @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = 'PAID'")
  BigDecimal sumPaidAmount();

  @Query(value = """
      SELECT YEAR(completed_at), MONTH(completed_at), COALESCE(SUM(amount), 0)
      FROM payment
      WHERE status = 'PAID' AND completed_at >= :from AND completed_at < :toExclusive
      GROUP BY YEAR(completed_at), MONTH(completed_at)
      ORDER BY YEAR(completed_at), MONTH(completed_at)
      """, nativeQuery = true)
  List<Object[]> aggregatePaidByMonth(
      @Param("from") Date from, @Param("toExclusive") Date toExclusive);
}
