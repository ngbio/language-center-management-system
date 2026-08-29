package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.LevelRequest;
import com.ntt.language_center_management.dto.response.LevelResponse;
import java.util.List;

public interface LevelService {
  List<LevelResponse> getAll(Integer languageId);

  List<LevelResponse> getAll(Integer languageId, String status);

  List<LevelResponse> getActive(Integer languageId);

  LevelResponse getById(Integer id);

  LevelResponse getActiveById(Integer id);

  LevelRequest getRequestById(Integer id);

  LevelResponse save(LevelRequest request);

  LevelResponse changeStatus(Integer id, String status);

  void delete(Integer id);
}
