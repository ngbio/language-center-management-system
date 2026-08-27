package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Courseclass;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseClassRepository
    extends JpaRepository<Courseclass, Integer>, JpaSpecificationExecutor<Courseclass> {

  boolean existsByClassCodeIgnoreCase(String classCode);

  boolean existsByClassCodeIgnoreCaseAndIdNot(String classCode, Integer id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Courseclass c where c.id = :id")
  Optional<Courseclass> lockById(@Param("id") Integer id);

  java.util.List<Courseclass> findByTeacherId_IdOrderByStartDateDesc(Integer teacherId);
}
