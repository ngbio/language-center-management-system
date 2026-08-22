package com.ntt.language_center_management.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ntt.language_center_management.entity.Level;

public interface LevelRepository extends JpaRepository<Level, Integer> {

    boolean existsByLanguageId_Id(Integer languageId);
}
