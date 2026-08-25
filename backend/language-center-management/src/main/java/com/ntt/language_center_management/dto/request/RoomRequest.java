package com.ntt.language_center_management.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RoomRequest {
  private Integer id;

  @NotBlank
  @Size(max = 30)
  private String roomCode;

  @NotBlank
  @Size(max = 100)
  private String roomName;

  @Min(1)
  private int capacity;

  @Size(max = 255)
  private String location;

  @NotBlank
  @Pattern(regexp = "ACTIVE|MAINTENANCE|INACTIVE")
  private String status = "ACTIVE";

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getRoomCode() {
    return roomCode;
  }

  public void setRoomCode(String roomCode) {
    this.roomCode = roomCode;
  }

  public String getRoomName() {
    return roomName;
  }

  public void setRoomName(String roomName) {
    this.roomName = roomName;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
