package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.dto.response.LevelResponse;
import com.ntt.language_center_management.entity.Level;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
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

  public LevelServiceImpl(LevelRepository levelRepository, LanguageRepository languageRepository) {
    this.levelRepository = levelRepository;
    this.languageRepository = languageRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LevelResponse> getAll(Integer languageId) {
    var levels =
        languageId == null
            ? levelRepository.findAll()
            : levelRepository.findByLanguageId_IdOrderByDisplayOrderAsc(languageId);
    return levels.stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public LevelResponse getById(Integer id) {
    return toResponse(find(id));
  }

  @Override
  @Transactional(readOnly = true)
  public LevelRequest getRequestById(Integer id) {
    Level value = find(id);
    LevelRequest request = new LevelRequest();
    request.setId(value.getId());
    request.setLanguageId(value.getLanguageId().getId());
    request.setLevelCode(value.getLevelCode());
    request.setLevelName(value.getLevelName());
    request.setDescription(value.getDescription());
    request.setDisplayOrder(value.getDisplayOrder());
    request.setStatus(value.getStatus());
    return request;
  }

  @Override
  public LevelResponse save(LevelRequest request) {
    String code = request.getLevelCode().trim().toUpperCase();
    Integer currentId = request.getId() == null ? -1 : request.getId();
    if (levelRepository.existsByLanguageId_IdAndLevelCodeIgnoreCaseAndIdNot(
        request.getLanguageId(), code, currentId))
      throw new DuplicateResourceException("Mã trình độ đã tồn tại trong ngôn ngữ này");
    var language =
        languageRepository
            .findById(request.getLanguageId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ngôn ngữ"));
    Level level = request.getId() == null ? new Level() : find(request.getId());
    level.setLanguageId(language);
    level.setLevelCode(code);
    level.setLevelName(request.getLevelName().trim());
    level.setDescription(
        StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
    level.setDisplayOrder(request.getDisplayOrder());
    level.setStatus(status(request.getStatus()));
    return toResponse(levelRepository.save(level));
  }

  @Override
  public void delete(Integer id) {
    Level level = find(id);
    if (level.getCourseList() != null && !level.getCourseList().isEmpty())
      throw new IllegalArgumentException("Không thể xóa trình độ đã có khóa học");
    levelRepository.delete(level);
  }

  private Level find(Integer id) {
    return levelRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy trình độ"));
  }

  private String status(String value) {
    String result = value == null ? "ACTIVE" : value.trim().toUpperCase();
    if (!STATUSES.contains(result)) throw new IllegalArgumentException("Trạng thái không hợp lệ");
    return result;
  }

  private LevelResponse toResponse(Level l) {
    var lang = l.getLanguageId();
    return new LevelResponse(
        l.getId(),
        l.getLevelCode(),
        l.getLevelName(),
        l.getDescription(),
        l.getDisplayOrder(),
        l.getStatus(),
        lang.getId(),
        lang.getLanguageCode(),
        lang.getLanguageName());
  }
}
