package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Teacher;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherRepository extends JpaRepository<Teacher, Integer> {
  Optional<Teacher> findByUserId_EmailIgnoreCase(String email);
}
