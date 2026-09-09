package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.enums.RefundStatus;

import com.ntt.language_center_management.entity.Refund;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, Integer> {
  Optional<Refund> findByIdempotencyKey(String idempotencyKey);
  List<Refund> findByEnrollment_IdOrderByCreatedAtDesc(Integer enrollmentId);
  List<Refund> findByEnrollment_StudentId_IdOrderByCreatedAtDesc(Integer studentId);
  List<Refund> findAllByOrderByCreatedAtDesc();
  List<Refund> findByStatusOrderByCreatedAtDesc(
      RefundStatus status);

  @Query("select coalesce(sum(r.amount), 0) from Refund r where r.status = 'COMPLETED'")
  BigDecimal sumCompletedAmount();

  @Query(value = """
      SELECT YEAR(completed_at), MONTH(completed_at), COALESCE(SUM(amount), 0)
      FROM refund
      WHERE status = 'COMPLETED' AND completed_at >= :from AND completed_at < :toExclusive
      GROUP BY YEAR(completed_at), MONTH(completed_at)
      ORDER BY YEAR(completed_at), MONTH(completed_at)
      """, nativeQuery = true)
  List<Object[]> aggregateCompletedByMonth(
      @Param("from") Date from, @Param("toExclusive") Date toExclusive);
}
