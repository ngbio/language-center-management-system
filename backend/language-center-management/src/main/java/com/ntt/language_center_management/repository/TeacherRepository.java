package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Teacher;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
  Optional<Teacher> findByUserId_EmailIgnoreCase(String email);

  @Query(
      "SELECT teacher FROM Teacher teacher JOIN FETCH teacher.userId user "
          + "WHERE user.status = :status ORDER BY user.fullName ASC")
  List<Teacher> findByUserStatus(@Param("status") String status);
}
