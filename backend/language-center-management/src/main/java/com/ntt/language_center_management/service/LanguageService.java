package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.dto.response.LanguageResponse;
import java.util.List;

public interface LanguageService {

  List<LanguageResponse> getLanguages();

  List<LanguageResponse> getLanguages(String status);

  List<LanguageResponse> getActiveLanguages();

  LanguageResponse getById(int id);

  LanguageResponse getActiveById(int id);

  LanguageRequest getLanguageById(int id);

  LanguageResponse addOrUpdateLanguage(LanguageRequest request);

  LanguageResponse changeStatus(int id, String status);

  boolean deleteLanguage(int id);
}
