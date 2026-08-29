package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.response.LanguageResponse;
import com.ntt.language_center_management.entity.Language;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.LanguageMapper;
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
  private final LanguageMapper languageMapper;

  public LanguageServiceImpl(
      LanguageRepository languageRepository,
      LevelRepository levelRepository,
      LanguageMapper languageMapper) {
    this.languageRepository = languageRepository;
    this.levelRepository = levelRepository;
    this.languageMapper = languageMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LanguageResponse> getLanguages() {
    return languageRepository.findAll().stream().map(languageMapper::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<LanguageResponse> getLanguages(String status) {
    if (!StringUtils.hasText(status)) {
      return getLanguages();
    }
    validateStatus(status);
    return languageRepository.findByStatusOrderByLanguageNameAsc(status).stream()
        .map(languageMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<LanguageResponse> getActiveLanguages() {
    return languageRepository.findByStatusOrderByLanguageNameAsc("ACTIVE").stream()
        .map(languageMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public LanguageResponse getById(int id) {
    return languageMapper.toResponse(findById(id));
  }

  @Override
  @Transactional(readOnly = true)
  public LanguageResponse getActiveById(int id) {
    Language language =
        languageRepository
            .findByIdAndStatus(id, "ACTIVE")
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngôn ngữ hoạt động"));
    return languageMapper.toResponse(language);
  }

  @Override
  @Transactional(readOnly = true)
  public LanguageRequest getLanguageById(int id) {
    return languageMapper.toRequest(findById(id));
  }

  @Override
  public LanguageResponse addOrUpdateLanguage(LanguageRequest request) {
    String languageCode = request.getLanguageCode().trim().toUpperCase();
    String languageName = request.getLanguageName().trim();
    String description =
        StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null;
    String status = StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE";

    validateRequest(request.getId(), languageCode, languageName, description, status);

    Language language = request.getId() == null ? new Language() : findById(request.getId());
    language.setLanguageCode(languageCode);
    language.setLanguageName(languageName);
    language.setDescription(description);
    language.setStatus(status);
    return languageMapper.toResponse(languageRepository.save(language));
  }

  @Override
  public LanguageResponse changeStatus(int id, String status) {
    validateStatus(status);
    Language language = findById(id);
    language.setStatus(status);
    return languageMapper.toResponse(languageRepository.save(language));
  }

  @Override
  public boolean deleteLanguage(int id) {
    Language language = findById(id);
    if (levelRepository.existsByLanguageId_Id(id)) {
      return false;
    }
    languageRepository.delete(language);
    return true;
  }

  private void validateRequest(
      Integer id, String languageCode, String languageName, String description, String status) {
    validateStatus(status);
    Integer currentId = id == null ? -1 : id;
    if (languageRepository.existsByLanguageCodeIgnoreCaseAndIdNot(languageCode, currentId)) {
      throw new DuplicateResourceException("Mã ngôn ngữ đã tồn tại");
    }
    if (languageRepository.existsByLanguageNameIgnoreCaseAndIdNot(languageName, currentId)) {
      throw new DuplicateResourceException("Tên ngôn ngữ đã tồn tại");
    }
    if (description != null && description.length() > 500) {
      throw new IllegalArgumentException("Mô tả không được vượt quá 500 ký tự");
    }
  }

  private void validateStatus(String status) {
    if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
      throw new IllegalArgumentException("Trạng thái phải là ACTIVE hoặc INACTIVE");
    }
  }

  private Language findById(Integer id) {
    return languageRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngôn ngữ"));
  }
}
