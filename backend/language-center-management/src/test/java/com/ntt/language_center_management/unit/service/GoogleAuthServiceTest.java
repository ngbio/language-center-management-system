package com.ntt.language_center_management.unit.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ntt.language_center_management.dto.request.GoogleLoginRequest;
import com.ntt.language_center_management.entity.GoogleAccount;
import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.GoogleAccountLinkRequiredException;
import com.ntt.language_center_management.exception.GoogleSignInUnavailableException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.*;
import com.ntt.language_center_management.service.GoogleAuthService;
import com.ntt.language_center_management.service.NotificationService;
import com.ntt.language_center_management.util.JwtUtils;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {
  @Mock FirebaseAuth firebaseAuth;
  @Mock FirebaseToken identity;
  @Mock GoogleAccountRepository googleAccounts;
  @Mock UserRepository users;
  @Mock StudentRepository students;
  @Mock RoleRepository roles;
  @Mock PasswordEncoder encoder;
  @Mock NotificationService notifications;
  @Mock ApplicationEventPublisher events;
  private final JwtUtils jwt = new JwtUtils("0123456789abcdef0123456789abcdef", 60000);
  private GoogleAuthService service;

  @BeforeEach
  void setUp() {
    service = createService(Optional.of(firebaseAuth));
  }

  private GoogleAuthService createService(Optional<FirebaseAuth> auth) {
    return new GoogleAuthService(auth, googleAccounts, users, students, roles,
        encoder, jwt, notifications, events);
  }

  private void verifiedGoogle() throws Exception {
    when(firebaseAuth.verifyIdToken("id-token", true)).thenReturn(identity);
    lenient().when(identity.getUid()).thenReturn("google-uid");
    lenient().when(identity.getEmail()).thenReturn("Student@Example.com");
    lenient().when(identity.isEmailVerified()).thenReturn(true);
    when(identity.getClaims()).thenReturn(Map.of("firebase", Map.of("sign_in_provider", "google.com")));
  }

  private User user(String roleCode, AccountStatus status) {
    Role role = new Role();
    role.setRoleCode(roleCode);
    role.setRoleName(roleCode);
    User user = new User(7);
    user.setRoleId(role);
    user.setEmail("student@example.com");
    user.setPasswordHash("existing-hash");
    user.setStatus(status);
    return user;
  }

  @Test
  void createsOnlyStudentWithProfileAndApplicationJwt() throws Exception {
    verifiedGoogle();
    when(identity.getName()).thenReturn("Google Student");
    Role role = user("STUDENT", AccountStatus.ACTIVE).getRoleId();
    when(roles.findByRoleCodeIgnoreCase("STUDENT")).thenReturn(Optional.of(role));
    when(encoder.encode(anyString())).thenReturn("unguessable-password-hash");
    when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> {
      User saved = invocation.getArgument(0);
      saved.setId(7);
      return saved;
    });

    var result = service.login(new GoogleLoginRequest("id-token", null));

    assertThat(result.roleCode()).isEqualTo("STUDENT");
    assertThat(jwt.validateTokenAndGetUsername(result.token())).isEqualTo("student@example.com");
    ArgumentCaptor<Student> profile = ArgumentCaptor.forClass(Student.class);
    verify(students).save(profile.capture());
    assertThat(profile.getValue().getStudentCode()).startsWith("HV");
    assertThat(profile.getValue().getUserId().getFullName()).isEqualTo("Google Student");
    verify(notifications).createWelcomeNotification(profile.getValue().getUserId());
    verify(events).publishEvent(any(Object.class));
    verify(googleAccounts).saveAndFlush(any(GoogleAccount.class));
  }

  @Test
  void linkedIdentityUsesStoredAccountEvenIfGoogleEmailChanges() throws Exception {
    verifiedGoogle();
    when(identity.getEmail()).thenReturn("changed@example.com");
    User user = user("TEACHER", AccountStatus.ACTIVE);
    when(googleAccounts.findByFirebaseUid("google-uid"))
        .thenReturn(Optional.of(new GoogleAccount("google-uid", user)));

    var result = service.login(new GoogleLoginRequest("id-token", null));

    assertThat(result.email()).isEqualTo("student@example.com");
    assertThat(result.roleCode()).isEqualTo("TEACHER");
    verifyNoInteractions(users, encoder, students);
  }

  @Test
  void requiresLocalPasswordBeforeLinkingMatchingEmail() throws Exception {
    verifiedGoogle();
    when(users.findByEmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(user("STUDENT", AccountStatus.ACTIVE)));

    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", null)))
        .isInstanceOf(GoogleAccountLinkRequiredException.class);
    verify(googleAccounts, never()).saveAndFlush(any());
    verify(users, never()).saveAndFlush(any());
  }

  @Test
  void rejectsWrongPasswordWithoutLinking() throws Exception {
    verifiedGoogle();
    when(users.findByEmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(user("STUDENT", AccountStatus.ACTIVE)));

    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", "wrong")))
        .isInstanceOf(UnauthorizedException.class);
    verify(googleAccounts, never()).saveAndFlush(any());
  }

  @Test
  void correctPasswordLinksExistingUserWithoutCreatingProfile() throws Exception {
    verifiedGoogle();
    when(users.findByEmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(user("TEACHER", AccountStatus.ACTIVE)));
    when(encoder.matches("correct", "existing-hash")).thenReturn(true);

    var result = service.login(new GoogleLoginRequest("id-token", "correct"));

    assertThat(result.userId()).isEqualTo(7);
    assertThat(result.roleCode()).isEqualTo("TEACHER");
    verify(googleAccounts).saveAndFlush(any(GoogleAccount.class));
    verifyNoInteractions(students, roles, notifications, events);
  }

  @ParameterizedTest
  @ValueSource(strings = {"INACTIVE", "LOCKED"})
  void refusesInactiveOrLockedLinkedAccounts(String status) throws Exception {
    verifiedGoogle();
    when(googleAccounts.findByFirebaseUid("google-uid")).thenReturn(Optional.of(
        new GoogleAccount("google-uid", user("STUDENT", AccountStatus.valueOf(status)))));
    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", null)))
        .isInstanceOf(UnauthorizedException.class);
    verifyNoInteractions(users, students);
  }

  @ParameterizedTest
  @ValueSource(strings = {"ADMIN", "CONSULTANT"})
  void refusesPrivilegedExistingAccounts(String role) throws Exception {
    verifiedGoogle();
    when(users.findByEmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(user(role, AccountStatus.ACTIVE)));
    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", "correct")))
        .isInstanceOf(UnauthorizedException.class);
    verify(googleAccounts, never()).saveAndFlush(any());
  }

  @Test
  void refusesSecondGoogleIdentityForSameUser() throws Exception {
    verifiedGoogle();
    when(users.findByEmailIgnoreCase("student@example.com"))
        .thenReturn(Optional.of(user("STUDENT", AccountStatus.ACTIVE)));
    when(googleAccounts.existsByUser_Id(7)).thenReturn(true);
    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", "correct")))
        .isInstanceOf(UnauthorizedException.class);
    verifyNoInteractions(encoder);
    verify(googleAccounts, never()).saveAndFlush(any());
  }

  @Test
  void rejectsNonGoogleFirebaseTokenIncludingChatCustomTokens() throws Exception {
    verifiedGoogle();
    when(identity.getClaims()).thenReturn(Map.of("firebase", Map.of("sign_in_provider", "custom")));
    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", null)))
        .isInstanceOf(UnauthorizedException.class);
    verifyNoInteractions(users, googleAccounts);
  }

  @Test
  void rejectsUnverifiedEmail() throws Exception {
    verifiedGoogle();
    when(identity.isEmailVerified()).thenReturn(false);
    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", null)))
        .isInstanceOf(UnauthorizedException.class);
    verifyNoInteractions(users, googleAccounts);
  }

  @Test
  void rejectsInvalidExpiredOrRevokedTokens() throws Exception {
    when(firebaseAuth.verifyIdToken("id-token", true)).thenThrow(mock(FirebaseAuthException.class));
    assertThatThrownBy(() -> service.login(new GoogleLoginRequest("id-token", null)))
        .isInstanceOf(UnauthorizedException.class);
    verifyNoInteractions(users, googleAccounts);
  }

  @Test
  void requestDoesNotExposeCredentialsInDebugLogs() {
    assertThat(new GoogleLoginRequest("private-token", "private-password").toString())
        .doesNotContain("private-token", "private-password");
  }

  @Test
  void disabledFirebaseDoesNotPreventStartupAndReturnsServiceUnavailable() {
    assertThatThrownBy(() -> createService(Optional.empty()).login(new GoogleLoginRequest("id-token", null)))
        .isInstanceOf(GoogleSignInUnavailableException.class);
    verifyNoInteractions(users, googleAccounts);
  }
}
