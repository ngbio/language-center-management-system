package com.ntt.language_center_management.service;

import java.util.List;

import com.ntt.language_center_management.dto.request.LanguageRequest;
import com.ntt.language_center_management.entity.Language;

public interface LanguageService {

    List<Language> getLanguages();

    LanguageRequest getLanguageById(int id);

    Language addOrUpdateLanguage(LanguageRequest request);

    boolean deleteLanguage(int id);
}
