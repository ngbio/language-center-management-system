package com.ntt.language_center_management.service.impl;

import com.ntt.language_center_management.dto.response.NotificationResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.entity.Notification;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.NotificationType;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.repository.NotificationRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.NotificationService;
import java.security.Principal;
import java.util.Date;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class NotificationServiceImpl implements NotificationService {
  private final NotificationRepository notifications;
  private final CurrentUserResolver currentUserResolver;

  public NotificationServiceImpl(
      NotificationRepository notifications, CurrentUserResolver currentUserResolver) {
    this.notifications = notifications;
    this.currentUserResolver = currentUserResolver;
  }

  @Override
  public void createWelcomeNotification(User user) {
    Notification notification = new Notification();
    notification.setUserId(user);
    notification.setTitle("Đăng ký tài khoản thành công");
    notification.setContent(
        "Chào mừng bạn đến với Lingua Center. Bạn có thể xem khóa học và đăng ký lớp ngay bây giờ.");
    notification.setNotificationType(NotificationType.SYSTEM);
    notification.setIsRead(false);
    notification.setCreatedAt(new Date());
    notification.setDedupKey("WELCOME:" + user.getId());
    notifications.save(notification);
  }

  @Override
  @Transactional(readOnly = true)
  public PageResponse<NotificationResponse> getMine(Principal principal, int page, int size) {
    if (page < 0) throw new IllegalArgumentException("Số trang không được nhỏ hơn 0");
    if (size < 1 || size > 50) {
      throw new IllegalArgumentException("Kích thước trang phải từ 1 đến 50");
    }
    User user = currentUserResolver.requireUser(principal);
    return PageResponse.from(
        notifications
            .findByUserId_IdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size))
            .map(this::toResponse));
  }

  @Override
  @Transactional(readOnly = true)
  public long countUnread(Principal principal) {
    return notifications.countByUserId_IdAndIsReadFalse(
        currentUserResolver.requireUser(principal).getId());
  }

  @Override
  public NotificationResponse markRead(Integer notificationId, Principal principal) {
    User user = currentUserResolver.requireUser(principal);
    Notification notification =
        notifications
            .findByIdAndUserId_Id(notificationId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));
    if (!notification.getIsRead()) {
      notification.setIsRead(true);
      notification.setReadAt(new Date());
      notification = notifications.save(notification);
    }
    return toResponse(notification);
  }

  @Override
  public int markAllRead(Principal principal) {
    User user = currentUserResolver.requireUser(principal);
    return notifications.markAllRead(user.getId(), new Date());
  }

  private NotificationResponse toResponse(Notification notification) {
    return new NotificationResponse(
        notification.getId(), notification.getTitle(), notification.getContent(),
        notification.getNotificationType(), notification.getIsRead(), notification.getCreatedAt(),
        notification.getReadAt());
  }
}
