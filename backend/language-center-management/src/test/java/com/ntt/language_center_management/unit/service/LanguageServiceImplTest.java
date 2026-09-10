package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.entity.Language;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.mapper.LanguageMapper;
import com.ntt.language_center_management.repository.LanguageRepository;
import com.ntt.language_center_management.repository.LevelRepository;
import com.ntt.language_center_management.service.impl.LanguageServiceImpl;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LanguageServiceImplTest {
  @Test void shouldNormalizeAndSaveWhenLanguageIsNew() {
    LanguageRepository repository = mock(LanguageRepository.class);
    LanguageServiceImpl service = new LanguageServiceImpl(repository, mock(LevelRepository.class), new LanguageMapper());
    when(repository.save(any(Language.class))).thenAnswer(call -> call.getArgument(0));
    var response = service.addOrUpdateLanguage(request(" en ", " English "));
    assertEquals("EN", response.languageCode());
    assertEquals("English", response.languageName());
  }

  @Test void shouldRejectSaveWhenLanguageCodeIsDuplicated() {
    LanguageRepository repository = mock(LanguageRepository.class);
    when(repository.existsByLanguageCodeIgnoreCaseAndIdNot("EN", -1)).thenReturn(true);
    var service = new LanguageServiceImpl(repository, mock(LevelRepository.class), new LanguageMapper());
    assertThrows(DuplicateResourceException.class, () -> service.addOrUpdateLanguage(request("EN", "English")));
    verify(repository, never()).save(any());
  }

  @Test void shouldReturnFalseWhenDeletingLanguageWithLevels() {
    LanguageRepository repository = mock(LanguageRepository.class);
    LevelRepository levels = mock(LevelRepository.class);
    Language language = language();
    when(repository.findById(1)).thenReturn(Optional.of(language));
    when(levels.existsByLanguageId_Id(1)).thenReturn(true);
    assertFalse(new LanguageServiceImpl(repository, levels, new LanguageMapper()).deleteLanguage(1));
    verify(repository, never()).delete(language);
  }

  @Test void shouldDeleteLanguageWhenNoLevelExists() {
    LanguageRepository repository = mock(LanguageRepository.class);
    LevelRepository levels = mock(LevelRepository.class);
    Language language = language();
    when(repository.findById(1)).thenReturn(Optional.of(language));
    assertTrue(new LanguageServiceImpl(repository, levels, new LanguageMapper()).deleteLanguage(1));
    verify(repository).delete(language);
  }

  private LanguageRequest request(String code, String name) {
    LanguageRequest value = new LanguageRequest(); value.setLanguageCode(code); value.setLanguageName(name);
    value.setStatus(CatalogStatus.ACTIVE); return value;
  }
  private Language language() {
    Language value = new Language(1); value.setLanguageCode("EN"); value.setLanguageName("English");
    value.setStatus(CatalogStatus.ACTIVE); return value;
  }
}
