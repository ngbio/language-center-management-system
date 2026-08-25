package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.request.RoomRequest;
import com.ntt.language_center_management.dto.response.RoomResponse;
import java.util.List;

public interface RoomService {
  List<RoomResponse> getAll();

  RoomResponse getById(Integer id);

  RoomRequest getRequestById(Integer id);

  RoomResponse save(RoomRequest request);

  void delete(Integer id);
}
