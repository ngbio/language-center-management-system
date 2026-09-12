package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.NotificationResponse;
import com.ntt.language_center_management.dto.response.PageResponse;
import com.ntt.language_center_management.entity.User;
import java.security.Principal;

public interface NotificationService {
  void createWelcomeNotification(User user);

  PageResponse<NotificationResponse> getMine(Principal principal, int page, int size);

  long countUnread(Principal principal);

  NotificationResponse markRead(Integer notificationId, Principal principal);

  int markAllRead(Principal principal);
}
