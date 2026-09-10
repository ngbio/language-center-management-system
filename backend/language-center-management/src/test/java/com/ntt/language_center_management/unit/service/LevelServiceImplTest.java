package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.entity.Course;
import com.ntt.language_center_management.entity.Language;
import com.ntt.language_center_management.entity.Level;
import com.ntt.language_center_management.enums.CatalogStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.LevelMapper;
import com.ntt.language_center_management.repository.LanguageRepository;
import com.ntt.language_center_management.repository.LevelRepository;
import com.ntt.language_center_management.service.impl.LevelServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LevelServiceImplTest {

  @Mock private LevelRepository levelRepository;
  @Mock private LanguageRepository languageRepository;
  private LevelServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new LevelServiceImpl(levelRepository, languageRepository, new LevelMapper());
  }

  @Test
  void shouldNormalizeAndSaveLevelWhenRequestIsValid() {
    Language language = language();
    LevelRequest request = request();
    when(languageRepository.findByIdAndStatus(1, CatalogStatus.ACTIVE))
        .thenReturn(Optional.of(language));
    when(levelRepository.save(any(Level.class))).thenAnswer(call -> call.getArgument(0));

    var response = service.save(request);

    assertThat(response.levelCode()).isEqualTo("A1");
    assertThat(response.levelName()).isEqualTo("Beginner");
    assertThat(response.description()).isNull();
    verify(levelRepository).save(any(Level.class));
  }

  @Test
  void shouldRejectDuplicateCodeWithinLanguage() {
    LevelRequest request = request();
    when(levelRepository.existsByLanguageId_IdAndLevelCodeIgnoreCaseAndIdNot(1, "A1", -1))
        .thenReturn(true);

    assertThatThrownBy(() -> service.save(request))
        .isInstanceOf(DuplicateResourceException.class);
    verify(levelRepository, never()).save(any());
  }

  @Test
  void shouldRejectDuplicateDisplayOrderWithinLanguage() {
    LevelRequest request = request();
    when(levelRepository.existsByLanguageId_IdAndDisplayOrderAndIdNot(1, 1, -1))
        .thenReturn(true);

    assertThatThrownBy(() -> service.save(request))
        .isInstanceOf(DuplicateResourceException.class);
  }

  @Test
  void shouldRejectCreationForInactiveOrMissingLanguage() {
    LevelRequest request = request();
    when(languageRepository.findByIdAndStatus(1, CatalogStatus.ACTIVE))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.save(request))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldRejectUnsupportedStatusFilter() {
    assertThatThrownBy(() -> service.getAll(null, "ARCHIVED"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void shouldThrowWhenLevelDoesNotExist() {
    when(levelRepository.findById(99)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getById(99))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shouldRejectDeletionWhenLevelHasCourses() {
    Level level = level();
    level.setCourseList(List.of(new Course()));
    when(levelRepository.findById(2)).thenReturn(Optional.of(level));

    assertThatThrownBy(() -> service.delete(2)).isInstanceOf(IllegalArgumentException.class);
    verify(levelRepository, never()).delete(level);
  }

  private LevelRequest request() {
    LevelRequest request = new LevelRequest();
    request.setLanguageId(1);
    request.setLevelCode(" a1 ");
    request.setLevelName(" Beginner ");
    request.setDescription("   ");
    request.setDisplayOrder(1);
    request.setStatus(CatalogStatus.ACTIVE);
    return request;
  }

  private Language language() {
    Language language = new Language(1);
    language.setLanguageCode("EN");
    language.setLanguageName("English");
    language.setStatus(CatalogStatus.ACTIVE);
    return language;
  }

  private Level level() {
    Level level = new Level(2);
    level.setLanguageId(language());
    level.setLevelCode("A1");
    level.setLevelName("Beginner");
    level.setDisplayOrder(1);
    level.setStatus(CatalogStatus.ACTIVE);
    return level;
  }
}
