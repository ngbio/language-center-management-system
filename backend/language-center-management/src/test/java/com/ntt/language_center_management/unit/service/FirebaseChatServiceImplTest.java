package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.firebase.auth.FirebaseAuth;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.ChatUnavailableException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.service.impl.FirebaseChatServiceImpl;
import java.security.Principal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FirebaseChatServiceImplTest {
  @Mock private UserService userService;
  @Mock private UserRepository userRepository;
  @Mock private FirebaseAuth firebaseAuth;

  private FirebaseChatServiceImpl service;
  private final Principal principal = () -> "student@example.com";

  @BeforeEach
  void setUp() {
    service = new FirebaseChatServiceImpl(userService, userRepository, Optional.of(firebaseAuth));
  }

  @Test
  void shouldCreateStudentTokenWithAssignedConsultantClaim() throws Exception {
    UserResponse student = response(5, "Student", "student@example.com", "STUDENT");
    User consultant = new User(2);
    consultant.setFullName("Consultant Anh");
    when(userService.getCurrentUserProfile(principal)).thenReturn(student);
    when(userRepository.findAllByRoleId_RoleCodeIgnoreCaseAndStatusOrderByIdAsc(
            "CONSULTANT", AccountStatus.ACTIVE))
        .thenReturn(List.of(consultant));
    when(firebaseAuth.createCustomToken(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyMap()))
        .thenReturn("firebase-token");

    var result = service.createToken(principal);

    assertThat(result.customToken()).isEqualTo("firebase-token");
    assertThat(result.uid()).isEqualTo("lcm-user-5");
    assertThat(result.consultantUid()).isEqualTo("lcm-user-2");
    assertThat(result.consultantName()).isEqualTo("Consultant Anh");
    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> claims = ArgumentCaptor.forClass(Map.class);
    verify(firebaseAuth).createCustomToken(org.mockito.ArgumentMatchers.eq("lcm-user-5"), claims.capture());
    assertThat(claims.getValue())
        .containsEntry("role", "STUDENT")
        .containsEntry("consultantUid", "lcm-user-2");
  }

  @Test
  void shouldUseOwnUidForConsultantTokenWithoutQueryingAssignment() throws Exception {
    when(userService.getCurrentUserProfile(principal))
        .thenReturn(response(7, "Consultant", "staff@example.com", "CONSULTANT"));
    when(firebaseAuth.createCustomToken(org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyMap()))
        .thenReturn("staff-token");

    var result = service.createToken(principal);

    assertThat(result.consultantUid()).isEqualTo("lcm-user-7");
    verify(userRepository, never())
        .findAllByRoleId_RoleCodeIgnoreCaseAndStatusOrderByIdAsc(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  void shouldRejectWhenPrincipalIsMissing() {
    assertThatThrownBy(() -> service.createToken(null)).isInstanceOf(UnauthorizedException.class);
  }

  @Test
  void shouldRejectRoleOutsideStudentAndConsultant() {
    when(userService.getCurrentUserProfile(principal))
        .thenReturn(response(1, "Admin", "admin@example.com", "ADMIN"));
    assertThatThrownBy(() -> service.createToken(principal)).isInstanceOf(ForbiddenException.class);
  }

  @Test
  void shouldReportUnavailableWhenFirebaseIsDisabled() {
    service = new FirebaseChatServiceImpl(userService, userRepository, Optional.empty());
    when(userService.getCurrentUserProfile(principal))
        .thenReturn(response(5, "Student", "student@example.com", "STUDENT"));
    assertThatThrownBy(() -> service.createToken(principal))
        .isInstanceOf(ChatUnavailableException.class)
        .hasMessageContaining("chưa được cấu hình");
  }

  @Test
  void shouldReportUnavailableWhenNoConsultantIsActive() {
    when(userService.getCurrentUserProfile(principal))
        .thenReturn(response(5, "Student", "student@example.com", "STUDENT"));
    when(userRepository.findAllByRoleId_RoleCodeIgnoreCaseAndStatusOrderByIdAsc(
            "CONSULTANT", AccountStatus.ACTIVE))
        .thenReturn(List.of());
    assertThatThrownBy(() -> service.createToken(principal))
        .isInstanceOf(ChatUnavailableException.class)
        .hasMessageContaining("chưa có nhân viên tư vấn");
  }

  private UserResponse response(Integer id, String name, String email, String role) {
    return new UserResponse(
        id, email, name, email, null, null, role, role, AccountStatus.ACTIVE, new Date(), null);
  }
}
