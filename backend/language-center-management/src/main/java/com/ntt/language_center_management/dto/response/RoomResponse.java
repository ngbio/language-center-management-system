package com.ntt.language_center_management.dto.response;

import com.ntt.language_center_management.enums.RoomStatus;

public record RoomResponse(
    Integer id, String roomCode, String roomName, int capacity, String location, RoomStatus status) {}
