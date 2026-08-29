package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.dto.response.LevelResponse;
import com.ntt.language_center_management.entity.Level;
import org.springframework.stereotype.Component;

@Component
public class LevelMapper {

  public LevelResponse toResponse(Level level) {
    var language = level.getLanguageId();
    return new LevelResponse(
        level.getId(),
        level.getLevelCode(),
        level.getLevelName(),
        level.getDescription(),
        level.getDisplayOrder(),
        level.getStatus(),
        language.getId(),
        language.getLanguageCode(),
        language.getLanguageName());
  }

  public LevelRequest toRequest(Level level) {
    LevelRequest request = new LevelRequest();
    request.setId(level.getId());
    request.setLanguageId(level.getLanguageId().getId());
    request.setLevelCode(level.getLevelCode());
    request.setLevelName(level.getLevelName());
    request.setDescription(level.getDescription());
    request.setDisplayOrder(level.getDisplayOrder());
    request.setStatus(level.getStatus());
    return request;
  }
}
