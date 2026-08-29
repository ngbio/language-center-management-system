package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.request.RoomRequest;
import com.ntt.language_center_management.dto.response.RoomResponse;
import com.ntt.language_center_management.entity.Room;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.repository.RoomRepository;
import com.ntt.language_center_management.service.RoomService;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class RoomServiceImpl implements RoomService {
  private static final Set<String> STATUSES = Set.of("ACTIVE", "MAINTENANCE", "INACTIVE");
  private final RoomRepository roomRepository;

  public RoomServiceImpl(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoomResponse> getAll() {
    return roomRepository.findAll().stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<RoomResponse> getAll(String status) {
    if (!StringUtils.hasText(status)) {
      return getAll();
    }
    String validStatus = status(status);
    return roomRepository.findByStatusOrderByRoomCodeAsc(validStatus).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public RoomResponse getById(Integer id) {
    return toResponse(find(id));
  }

  @Override
  @Transactional(readOnly = true)
  public RoomRequest getRequestById(Integer id) {
    Room value = find(id);
    RoomRequest request = new RoomRequest();
    request.setId(value.getId());
    request.setRoomCode(value.getRoomCode());
    request.setRoomName(value.getRoomName());
    request.setCapacity(value.getCapacity());
    request.setLocation(value.getLocation());
    request.setStatus(value.getStatus());
    return request;
  }

  @Override
  public RoomResponse save(RoomRequest request) {
    String code = request.getRoomCode().trim().toUpperCase();
    if (request.getId() == null
        ? roomRepository.existsByRoomCodeIgnoreCase(code)
        : roomRepository.existsByRoomCodeIgnoreCaseAndIdNot(code, request.getId()))
      throw new DuplicateResourceException("Mã phòng đã tồn tại");
    Room room = request.getId() == null ? new Room() : find(request.getId());
    room.setRoomCode(code);
    room.setRoomName(request.getRoomName().trim());
    room.setCapacity(request.getCapacity());
    room.setLocation(
        StringUtils.hasText(request.getLocation()) ? request.getLocation().trim() : null);
    room.setStatus(status(request.getStatus()));
    return toResponse(roomRepository.save(room));
  }

  @Override
  public void delete(Integer id) {
    Room room = find(id);
    if (room.getClassscheduleList() != null && !room.getClassscheduleList().isEmpty())
      throw new IllegalArgumentException("Không thể xóa phòng đã có lịch học");
    roomRepository.delete(room);
  }

  private Room find(Integer id) {
    return roomRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng"));
  }

  private String status(String value) {
    String result = value == null ? "ACTIVE" : value.trim().toUpperCase();
    if (!STATUSES.contains(result))
      throw new IllegalArgumentException("Trạng thái phòng không hợp lệ");
    return result;
  }

  private RoomResponse toResponse(Room r) {
    return new RoomResponse(
        r.getId(),
        r.getRoomCode(),
        r.getRoomName(),
        r.getCapacity(),
        r.getLocation(),
        r.getStatus());
  }
}
