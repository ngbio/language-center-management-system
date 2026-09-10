package com.ntt.language_center_management.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ntt.language_center_management.dto.request.RoomRequest;
import com.ntt.language_center_management.entity.Room;
import com.ntt.language_center_management.enums.RoomStatus;
import com.ntt.language_center_management.exception.DuplicateResourceException;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.repository.RoomRepository;
import com.ntt.language_center_management.service.impl.RoomServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoomServiceImplTest {
  @Test void shouldNormalizeAndSaveWhenRoomIsValid() {
    RoomRepository repository = mock(RoomRepository.class);
    when(repository.save(any(Room.class))).thenAnswer(call -> call.getArgument(0));
    var response = new RoomServiceImpl(repository).save(request(" p101 "));
    assertEquals("P101", response.roomCode());
    assertEquals("Phòng 101", response.roomName());
  }

  @Test void shouldRejectSaveWhenRoomCodeIsDuplicated() {
    RoomRepository repository = mock(RoomRepository.class);
    when(repository.existsByRoomCodeIgnoreCase("P101")).thenReturn(true);
    assertThrows(DuplicateResourceException.class, () -> new RoomServiceImpl(repository).save(request("P101")));
  }

  @Test void shouldRejectDeletionWhenRoomHasSchedules() {
    RoomRepository repository = mock(RoomRepository.class);
    Room room = new Room(1); room.setClassscheduleList(List.of(mock(com.ntt.language_center_management.entity.Classschedule.class)));
    when(repository.findById(1)).thenReturn(Optional.of(room));
    assertThrows(IllegalArgumentException.class, () -> new RoomServiceImpl(repository).delete(1));
    verify(repository, never()).delete(room);
  }

  @Test void shouldThrowWhenRoomDoesNotExist() {
    RoomRepository repository = mock(RoomRepository.class);
    when(repository.findById(99)).thenReturn(Optional.empty());
    assertThrows(ResourceNotFoundException.class, () -> new RoomServiceImpl(repository).getById(99));
  }

  private RoomRequest request(String code) {
    RoomRequest value = new RoomRequest(); value.setRoomCode(code); value.setRoomName(" Phòng 101 ");
    value.setCapacity(20); value.setStatus(RoomStatus.ACTIVE); return value;
  }
}
