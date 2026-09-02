package com.ntt.language_center_management.repository;

import com.ntt.language_center_management.entity.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Integer> {

  List<Room> findByStatusOrderByRoomCodeAsc(String status);

  Optional<Room> findByIdAndStatus(Integer id, String status);

  Optional<Room> findByRoomCodeIgnoreCase(String roomCode);

  boolean existsByRoomCodeIgnoreCase(String roomCode);

  boolean existsByRoomCodeIgnoreCaseAndIdNot(String roomCode, Integer id);
}
