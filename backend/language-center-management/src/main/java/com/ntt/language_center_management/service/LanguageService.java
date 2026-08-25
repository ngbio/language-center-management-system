package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.response.LanguageResponse;
import java.util.List;

public interface LanguageService {

  List<LanguageResponse> getLanguages();

  LanguageResponse getById(int id);

  LanguageRequest getLanguageById(int id);

  LanguageResponse addOrUpdateLanguage(LanguageRequest request);

  boolean deleteLanguage(int id);
}
