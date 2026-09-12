package com.ntt.language_center_management.controller.student;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.NotificationResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.dto.response.UnreadNotificationCountResponse;
import com.ntt.language_center_management.service.NotificationService;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students/me/notifications")
public class NotificationApiController {
  private final NotificationService notifications;

  public NotificationApiController(NotificationService notifications) {
    this.notifications = notifications;
  }

  @GetMapping
  public ApiResponse<PageResponse<NotificationResponse>> getMine(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      Principal principal) {
    return new ApiResponse<>(200, "Lấy danh sách thông báo thành công",
        notifications.getMine(principal, page, size));
  }

  @GetMapping("/unread-count")
  public ApiResponse<UnreadNotificationCountResponse> unreadCount(Principal principal) {
    return new ApiResponse<>(200, "Lấy số thông báo chưa đọc thành công",
        new UnreadNotificationCountResponse(notifications.countUnread(principal)));
  }

  @PatchMapping("/{id}/read")
  public ApiResponse<NotificationResponse> markRead(
      @PathVariable Integer id, Principal principal) {
    return new ApiResponse<>(200, "Đánh dấu thông báo đã đọc thành công",
        notifications.markRead(id, principal));
  }

  @PatchMapping("/read-all")
  public ApiResponse<Integer> markAllRead(Principal principal) {
    return new ApiResponse<>(200, "Đánh dấu tất cả thông báo đã đọc thành công",
        notifications.markAllRead(principal));
  }
}
