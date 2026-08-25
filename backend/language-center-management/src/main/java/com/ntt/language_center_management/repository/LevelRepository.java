package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Level;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevelRepository extends JpaRepository<Level, Integer> {

  boolean existsByLanguageId_Id(Integer languageId);

  List<Level> findByLanguageId_IdOrderByDisplayOrderAsc(Integer languageId);

  Optional<Level> findByLanguageId_IdAndLevelCodeIgnoreCase(Integer languageId, String levelCode);

  boolean existsByLanguageId_IdAndLevelCodeIgnoreCase(Integer languageId, String levelCode);

  boolean existsByLanguageId_IdAndLevelCodeIgnoreCaseAndIdNot(
      Integer languageId, String levelCode, Integer id);
}
