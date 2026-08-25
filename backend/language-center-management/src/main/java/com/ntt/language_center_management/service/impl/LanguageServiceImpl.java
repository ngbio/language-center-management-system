package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.response.LanguageResponse;
import com.ntt.language_center_management.entity.Language;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.repository.LanguageRepository;
import com.ntt.language_center_management.repository.LevelRepository;
import com.ntt.language_center_management.service.LanguageService;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class LanguageServiceImpl implements LanguageService {

  private final LanguageRepository languageRepository;
  private final LevelRepository levelRepository;

  public LanguageServiceImpl(
      LanguageRepository languageRepository, LevelRepository levelRepository) {
    this.languageRepository = languageRepository;
    this.levelRepository = levelRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LanguageResponse> getLanguages() {
    return languageRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public LanguageResponse getById(int id) {
    return toResponse(findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public LanguageRequest getLanguageById(int id) {
    Language language = findById(id);

    LanguageRequest request = new LanguageRequest();
    request.setId(language.getId());
    request.setLanguageCode(language.getLanguageCode());
    request.setLanguageName(language.getLanguageName());
    request.setDescription(language.getDescription());
    request.setStatus(language.getStatus());
    return request;
  }

  @Override
  public LanguageResponse addOrUpdateLanguage(LanguageRequest request) {
    String languageCode = request.getLanguageCode() != null ? request.getLanguageCode().trim() : "";
    String languageName = request.getLanguageName() != null ? request.getLanguageName().trim() : "";
    String description = request.getDescription() != null ? request.getDescription().trim() : null;
    String status =
        StringUtils.hasText(request.getStatus())
            ? request.getStatus().trim().toUpperCase()
            : "ACTIVE";

    validateRequest(request.getId(), languageCode, languageName, description, status);

    Language language;
    if (request.getId() != null) {
      language = findById(request.getId());
    } else {
      language = new Language();
    }

    language.setLanguageCode(languageCode);
    language.setLanguageName(languageName);
    language.setDescription(StringUtils.hasText(description) ? description : null);
    language.setStatus(status);

    return toResponse(languageRepository.save(language));
  }

  @Override
  public boolean deleteLanguage(int id) {
    if (!languageRepository.existsById(id)) {
      throw new ResourceNotFoundException("Không tìm thấy ngôn ngữ");
    }

    if (levelRepository.existsByLanguageId_Id(id)) {
      return false;
    }

    languageRepository.deleteById(id);
    return true;
  }

  private void validateRequest(
      Integer id, String languageCode, String languageName, String description, String status) {
    if (!StringUtils.hasText(languageCode)) {
      throw new IllegalArgumentException("Mã ngôn ngữ không được để trống");
    }

    if (!StringUtils.hasText(languageName)) {
      throw new IllegalArgumentException("Tên ngôn ngữ không được để trống");
    }

    if (languageCode.length() > 20) {
      throw new IllegalArgumentException("Mã ngôn ngữ không được vượt quá 20 ký tự");
    }

    if (languageName.length() > 100) {
      throw new IllegalArgumentException("Tên ngôn ngữ không được vượt quá 100 ký tự");
    }

    if (description != null && description.length() > 500) {
      throw new IllegalArgumentException("Mô tả không được vượt quá 500 ký tự");
    }

    if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
      throw new IllegalArgumentException("Status phải là ACTIVE hoặc INACTIVE");
    }

    Integer currentId = id == null ? -1 : id;
    if (languageRepository.existsByLanguageCodeIgnoreCaseAndIdNot(languageCode, currentId)) {
      throw new DuplicateResourceException("Mã ngôn ngữ đã tồn tại");
    }

    if (languageRepository.existsByLanguageNameIgnoreCaseAndIdNot(languageName, currentId)) {
      throw new DuplicateResourceException("Tên ngôn ngữ đã tồn tại");
    }
  }

  private Language findById(Integer id) {
    return languageRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngôn ngữ"));
  }

  private LanguageResponse toResponse(Language language) {
    return new LanguageResponse(
        language.getId(),
        language.getLanguageCode(),
        language.getLanguageName(),
        language.getDescription(),
        language.getStatus());
  }
}
