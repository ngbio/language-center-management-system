package com.ntt.language_center_management.mapper;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.response.LanguageResponse;
import com.ntt.language_center_management.entity.Language;
import org.springframework.stereotype.Component;

@Component
public class LanguageMapper {

  public LanguageResponse toResponse(Language language) {
    return new LanguageResponse(
        language.getId(),
        language.getLanguageCode(),
        language.getLanguageName(),
        language.getDescription(),
        language.getStatus());
  }

  public LanguageRequest toRequest(Language language) {
    LanguageRequest request = new LanguageRequest();
    request.setId(language.getId());
    request.setLanguageCode(language.getLanguageCode());
    request.setLanguageName(language.getLanguageName());
    request.setDescription(language.getDescription());
    request.setStatus(language.getStatus());
    return request;
  }
}
