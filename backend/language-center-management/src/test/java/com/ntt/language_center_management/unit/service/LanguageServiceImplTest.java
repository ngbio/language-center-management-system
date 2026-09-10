package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.entity.Language;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.LanguageMapper;
import com.ntt.language_center_management.repository.LanguageRepository;
import com.ntt.language_center_management.repository.LevelRepository;
import com.ntt.language_center_management.service.impl.LanguageServiceImpl;
import java.util.Optional;
import java.util.List;
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

  @Test void shouldRejectSaveWhenLanguageNameIsDuplicated() {
    LanguageRepository repository = mock(LanguageRepository.class);
    when(repository.existsByLanguageNameIgnoreCaseAndIdNot("English", -1)).thenReturn(true);
    var service = new LanguageServiceImpl(repository, mock(LevelRepository.class), new LanguageMapper());
    assertThrows(DuplicateResourceException.class,
        () -> service.addOrUpdateLanguage(request("EN", "English")));
    verify(repository, never()).save(any());
  }

  @Test void shouldKeepIdAndExcludeCurrentLanguageFromDuplicateChecksWhenUpdating() {
    LanguageRepository repository = mock(LanguageRepository.class);
    Language existing = language();
    LanguageRequest request = request(" en ", " English Updated ");
    request.setId(1);
    when(repository.findById(1)).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);

    var response = new LanguageServiceImpl(repository, mock(LevelRepository.class), new LanguageMapper())
        .addOrUpdateLanguage(request);

    assertEquals(1, response.id());
    assertEquals("English Updated", response.languageName());
    verify(repository).existsByLanguageCodeIgnoreCaseAndIdNot("EN", 1);
    verify(repository).existsByLanguageNameIgnoreCaseAndIdNot("English Updated", 1);
  }

  @Test void shouldRejectUnsupportedStatusFilter() {
    var service = new LanguageServiceImpl(mock(LanguageRepository.class),
        mock(LevelRepository.class), new LanguageMapper());
    assertThrows(IllegalArgumentException.class, () -> service.getLanguages("ARCHIVED"));
  }

  @Test void shouldThrowWhenLanguageDoesNotExist() {
    LanguageRepository repository = mock(LanguageRepository.class);
    when(repository.findById(99)).thenReturn(Optional.empty());
    var service = new LanguageServiceImpl(repository, mock(LevelRepository.class), new LanguageMapper());
    assertThrows(ResourceNotFoundException.class, () -> service.getById(99));
  }

  @Test void shouldReturnOnlyActiveLanguagesWhenGettingPublicCatalog() {
    LanguageRepository repository = mock(LanguageRepository.class);
    Language active = language();
    when(repository.findByStatusOrderByLanguageNameAsc(CatalogStatus.ACTIVE))
        .thenReturn(List.of(active));
    var result = new LanguageServiceImpl(repository, mock(LevelRepository.class), new LanguageMapper())
        .getActiveLanguages();
    assertEquals(1, result.size());
    assertEquals(CatalogStatus.ACTIVE, result.get(0).status());
    verify(repository).findByStatusOrderByLanguageNameAsc(CatalogStatus.ACTIVE);
    verify(repository, never()).findAll();
  }

  @Test void shouldUpdateAndMapStatusWhenChangingLanguageStatus() {
    LanguageRepository repository = mock(LanguageRepository.class);
    Language existing = language();
    when(repository.findById(1)).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);
    var response = new LanguageServiceImpl(repository, mock(LevelRepository.class), new LanguageMapper())
        .changeStatus(1, CatalogStatus.INACTIVE);
    assertEquals(CatalogStatus.INACTIVE, existing.getStatus());
    assertEquals(CatalogStatus.INACTIVE, response.status());
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
