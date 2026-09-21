package com.ntt.language_center_management.policy;

import com.ntt.language_center_management.enums.DeliveryMode;
import com.ntt.language_center_management.enums.RoomStatus;
import com.ntt.language_center_management.dto.request.ClassScheduleRequest;
import com.ntt.language_center_management.entity.Courseclass;
import com.ntt.language_center_management.entity.Room;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.repository.RoomRepository;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

/** Read-only business rules; callers retain transaction and persistence ownership. */
@Component
public class ScheduleLocationPolicy {
  private final RoomRepository roomRepository;
  public ScheduleLocationPolicy(RoomRepository roomRepository) {
    this.roomRepository = roomRepository;
  }

  public Room validateLocation(ClassScheduleRequest request, Courseclass courseClass) {
    if (request.deliveryMode() == DeliveryMode.IN_PERSON) {
      if (request.roomId() == null) {
        throw new IllegalArgumentException("Lịch học trực tiếp phải chọn phòng");
      }
      if (StringUtils.hasText(request.meetingUrl())) {
        throw new IllegalArgumentException("Lịch học trực tiếp không được có đường dẫn online");
      }
      Room room =
          roomRepository
              .findByIdAndStatus(request.roomId(), RoomStatus.ACTIVE)
              .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng đang hoạt động"));
      if (room.getCapacity() < courseClass.getMaxStudents()) {
        throw new IllegalArgumentException("Sức chứa phòng nhỏ hơn sĩ số tối đa của lớp");
      }
      return room;
    }

    if (request.roomId() != null) {
      throw new IllegalArgumentException("Lịch học online không được chọn phòng học");
    }
    if (!StringUtils.hasText(request.meetingUrl())) {
      throw new IllegalArgumentException("Lịch học online phải có đường dẫn phòng học");
    }
    return null;
  }
}
