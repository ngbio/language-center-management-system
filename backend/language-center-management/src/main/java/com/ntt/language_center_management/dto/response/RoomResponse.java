package com.ntt.language_center_management.dto.response;

public record RoomResponse(
    Integer id, String roomCode, String roomName, int capacity, String location, String status) {}
