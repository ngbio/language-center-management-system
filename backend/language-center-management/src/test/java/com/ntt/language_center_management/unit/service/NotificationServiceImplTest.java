package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.entity.Notification;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.NotificationType;
import com.ntt.language_center_management.exception.ResourceNotFoundException;
import com.ntt.language_center_management.repository.NotificationRepository;
import com.ntt.language_center_management.security.CurrentUserResolver;
import com.ntt.language_center_management.service.impl.NotificationServiceImpl;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class NotificationServiceImplTest {
  private NotificationRepository notifications;
  private CurrentUserResolver resolver;
  private NotificationServiceImpl service;
  private final Principal principal = () -> "student@example.com";
  private User user;

  @BeforeEach
  void setUp() {
    notifications = mock(NotificationRepository.class);
    resolver = mock(CurrentUserResolver.class);
    service = new NotificationServiceImpl(notifications, resolver);
    user = new User();
    user.setId(7);
    when(resolver.requireUser(principal)).thenReturn(user);
  }

  @Test
  void createsUnreadWelcomeNotificationWithStableDedupKey() {
    service.createWelcomeNotification(user);
    ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
    verify(notifications).save(captor.capture());
    assertThat(captor.getValue().getDedupKey()).isEqualTo("WELCOME:7");
    assertThat(captor.getValue().getNotificationType()).isEqualTo(NotificationType.SYSTEM);
    assertThat(captor.getValue().getIsRead()).isFalse();
  }

  @Test
  void listsOnlyCurrentUsersNotificationsAndCountsUnread() {
    Notification notification = notification(10, false);
    when(notifications.findByUserId_IdOrderByCreatedAtDesc(any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(List.of(notification)));
    when(notifications.countByUserId_IdAndIsReadFalse(7)).thenReturn(3L);

    assertThat(service.getMine(principal, 0, 20).content()).singleElement()
        .satisfies(item -> assertThat(item.id()).isEqualTo(10));
    assertThat(service.countUnread(principal)).isEqualTo(3);
    verify(notifications).findByUserId_IdOrderByCreatedAtDesc(any(), any(Pageable.class));
  }

  @Test
  void marksOwnedNotificationReadAndRejectsAnotherUsersNotification() {
    Notification notification = notification(10, false);
    when(notifications.findByIdAndUserId_Id(10, 7)).thenReturn(Optional.of(notification));
    when(notifications.save(notification)).thenReturn(notification);

    assertThat(service.markRead(10, principal).read()).isTrue();
    assertThat(notification.getReadAt()).isNotNull();
    when(notifications.findByIdAndUserId_Id(11, 7)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.markRead(11, principal))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void marksAllCurrentUsersNotificationsRead() {
    when(notifications.markAllRead(any(), any(Date.class))).thenReturn(4);
    assertThat(service.markAllRead(principal)).isEqualTo(4);
    verify(notifications).markAllRead(any(), any(Date.class));
  }

  private Notification notification(int id, boolean read) {
    Notification notification = new Notification();
    notification.setId(id);
    notification.setUserId(user);
    notification.setTitle("Thông báo");
    notification.setContent("Nội dung");
    notification.setNotificationType(NotificationType.SCHEDULE);
    notification.setIsRead(read);
    notification.setCreatedAt(new Date());
    return notification;
  }
}
