package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.dto.response.LevelResponse;
import com.ntt.language_center_management.entity.Language;
import com.ntt.language_center_management.entity.Level;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.mapper.LevelMapper;
import com.ntt.language_center_management.repository.LanguageRepository;
import com.ntt.language_center_management.repository.LevelRepository;
import com.ntt.language_center_management.service.LevelService;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class LevelServiceImpl implements LevelService {
  private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");

  private final LevelRepository levelRepository;
  private final LanguageRepository languageRepository;
  private final LevelMapper levelMapper;

  public LevelServiceImpl(
      LevelRepository levelRepository,
      LanguageRepository languageRepository,
      LevelMapper levelMapper) {
    this.levelRepository = levelRepository;
    this.languageRepository = languageRepository;
    this.levelMapper = levelMapper;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LevelResponse> getAll(Integer languageId) {
    List<Level> levels =
        languageId == null
            ? levelRepository.findAll()
            : levelRepository.findByLanguageId_IdOrderByDisplayOrderAsc(languageId);
    return levels.stream().map(levelMapper::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<LevelResponse> getAll(Integer languageId, String status) {
    if (!StringUtils.hasText(status)) {
      return getAll(languageId);
    }
    validateStatus(status);
    List<Level> levels =
        languageId == null
            ? levelRepository.findByStatusOrderByDisplayOrderAsc(status)
            : levelRepository.findByLanguageId_IdAndStatusOrderByDisplayOrderAsc(
                languageId, status);
    return levels.stream().map(levelMapper::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<LevelResponse> getActive(Integer languageId) {
    List<Level> levels;
    if (languageId == null) {
      levels =
          levelRepository.findByStatusAndLanguageId_StatusOrderByDisplayOrderAsc(
              "ACTIVE", "ACTIVE");
    } else {
      findActiveLanguage(languageId);
      levels =
          levelRepository.findByLanguageId_IdAndStatusOrderByDisplayOrderAsc(
              languageId, "ACTIVE");
    }
    return levels.stream().map(levelMapper::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public LevelResponse getById(Integer id) {
    return levelMapper.toResponse(find(id));
  }

  @Override
  @Transactional(readOnly = true)
  public LevelResponse getActiveById(Integer id) {
    Level level =
        levelRepository
            .findByIdAndStatus(id, "ACTIVE")
            .filter(value -> "ACTIVE".equals(value.getLanguageId().getStatus()))
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trình độ hoạt động"));
    return levelMapper.toResponse(level);
  }

  @Override
  @Transactional(readOnly = true)
  public LevelRequest getRequestById(Integer id) {
    return levelMapper.toRequest(find(id));
  }

  @Override
  public LevelResponse save(LevelRequest request) {
    String code = request.getLevelCode().trim().toUpperCase();
    Integer currentId = request.getId() == null ? -1 : request.getId();
    if (levelRepository.existsByLanguageId_IdAndLevelCodeIgnoreCaseAndIdNot(
        request.getLanguageId(), code, currentId)) {
      throw new DuplicateResourceException("Mã trình độ đã tồn tại trong ngôn ngữ này");
    }
    if (levelRepository.existsByLanguageId_IdAndDisplayOrderAndIdNot(
        request.getLanguageId(), request.getDisplayOrder(), currentId)) {
      throw new DuplicateResourceException("Thứ tự hiển thị đã tồn tại trong ngôn ngữ này");
    }

    Level level = request.getId() == null ? new Level() : find(request.getId());
    Language language;
    if (request.getId() == null
        || !level.getLanguageId().getId().equals(request.getLanguageId())) {
      language = findActiveLanguage(request.getLanguageId());
    } else {
      language = level.getLanguageId();
    }
    level.setLanguageId(language);
    level.setLevelCode(code);
    level.setLevelName(request.getLevelName().trim());
    level.setDescription(
        StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
    level.setDisplayOrder(request.getDisplayOrder());
    level.setStatus(validateStatus(request.getStatus()));
    return levelMapper.toResponse(levelRepository.save(level));
  }

  @Override
  public LevelResponse changeStatus(Integer id, String status) {
    Level level = find(id);
    level.setStatus(validateStatus(status));
    return levelMapper.toResponse(levelRepository.save(level));
  }

  @Override
  public void delete(Integer id) {
    Level level = find(id);
    if (level.getCourseList() != null && !level.getCourseList().isEmpty()) {
      throw new IllegalArgumentException("Không thể xóa trình độ đã có khóa học");
    }
    levelRepository.delete(level);
  }

  private Level find(Integer id) {
    return levelRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trình độ"));
  }

  private Language findActiveLanguage(Integer languageId) {
    return languageRepository
        .findByIdAndStatus(languageId, "ACTIVE")
        .orElseThrow(
            () -> new IllegalArgumentException("Chỉ được tạo trình độ cho ngôn ngữ hoạt động"));
  }

  private String validateStatus(String status) {
    String value = status == null ? "ACTIVE" : status;
    if (!STATUSES.contains(value)) {
      throw new IllegalArgumentException("Trạng thái phải là ACTIVE hoặc INACTIVE");
    }
    return value;
  }
}
