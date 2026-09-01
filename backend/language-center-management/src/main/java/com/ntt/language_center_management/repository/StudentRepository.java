package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Integer> {
  Optional<Student> findByUserId_EmailIgnoreCase(String email);
}
