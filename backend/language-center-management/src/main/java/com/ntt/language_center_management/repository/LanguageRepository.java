package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Language;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageRepository extends JpaRepository<Language, Integer> {
  List<Language> findByStatusOrderByLanguageNameAsc(String status);

  Optional<Language> findByIdAndStatus(Integer id, String status);

  Optional<Language> findByLanguageCodeIgnoreCase(String languageCode);

  Optional<Language> findByLanguageNameIgnoreCase(String languageName);

  boolean existsByLanguageCodeIgnoreCase(String languageCode);

  boolean existsByLanguageCodeIgnoreCaseAndIdNot(String languageCode, Integer id);

  boolean existsByLanguageNameIgnoreCaseAndIdNot(String languageName, Integer id);
}
