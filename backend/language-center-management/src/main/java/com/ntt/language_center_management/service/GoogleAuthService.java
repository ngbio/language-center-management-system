package com.ntt.language_center_management.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.ntt.language_center_management.dto.request.GoogleLoginRequest;
import com.ntt.language_center_management.dto.response.LoginResponse;
import com.ntt.language_center_management.entity.GoogleAccount;
import com.ntt.language_center_management.entity.Student;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.event.AccountCreatedMailEvent;
import com.ntt.language_center_management.exception.GoogleAccountLinkRequiredException;
import com.ntt.language_center_management.exception.GoogleSignInUnavailableException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.GoogleAccountRepository;
import com.ntt.language_center_management.repository.RoleRepository;
import com.ntt.language_center_management.repository.StudentRepository;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.util.JwtUtils;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GoogleAuthService {
  private final Optional<FirebaseAuth> firebaseAuth;
  private final GoogleAccountRepository googleAccounts;
  private final UserRepository users;
  private final StudentRepository students;
  private final RoleRepository roles;
  private final PasswordEncoder passwordEncoder;
  private final JwtUtils jwtUtils;
  private final NotificationService notifications;
  private final ApplicationEventPublisher events;

  public GoogleAuthService(Optional<FirebaseAuth> firebaseAuth,
      GoogleAccountRepository googleAccounts, UserRepository users,
      StudentRepository students, RoleRepository roles, PasswordEncoder passwordEncoder,
      JwtUtils jwtUtils, NotificationService notifications, ApplicationEventPublisher events) {
    this.firebaseAuth = firebaseAuth;
    this.googleAccounts = googleAccounts;
    this.users = users;
    this.students = students;
    this.roles = roles;
    this.passwordEncoder = passwordEncoder;
    this.jwtUtils = jwtUtils;
    this.notifications = notifications;
    this.events = events;
  }

  @Transactional
  public LoginResponse login(GoogleLoginRequest request) {
    FirebaseToken identity = verifyGoogleToken(request.idToken());
    User user;
    var linkedAccount = googleAccounts.findByFirebaseUid(identity.getUid());
    if (linkedAccount.isPresent()) {
      // Resolve by stable UID, never by mutable profile information from the browser.
      user = linkedAccount.get().getUser();
      validateAccount(user);
    } else {
      String email = identity.getEmail().trim().toLowerCase(Locale.ROOT);
      var existing = users.findByEmailIgnoreCase(email);
      if (existing.isPresent()) {
        user = existing.get();
        validateAccount(user);
        if (googleAccounts.existsByUser_Id(user.getId())) {
          throw new UnauthorizedException("Tài khoản đã liên kết với một tài khoản Google khác.");
        }
        if (request.password() == null || request.password().isBlank()) {
          throw new GoogleAccountLinkRequiredException();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
          throw new UnauthorizedException("Mật khẩu xác nhận liên kết không đúng.");
        }
      } else {
        user = createStudent(identity, email);
      }
      // Both identity and user uniqueness are also enforced by database constraints.
      googleAccounts.saveAndFlush(new GoogleAccount(identity.getUid(), user));
    }
    return new LoginResponse(jwtUtils.generateToken(user.getEmail()), user.getId(),
        user.getEmail(), user.getRoleId().getRoleName(), user.getRoleId().getRoleCode());
  }

  private FirebaseToken verifyGoogleToken(String idToken) {
    FirebaseAuth auth = firebaseAuth.orElseThrow(GoogleSignInUnavailableException::new);
    FirebaseToken token;
    try {
      token = auth.verifyIdToken(idToken, true);
    } catch (FirebaseAuthException | IllegalArgumentException exception) {
      throw new UnauthorizedException("Phiên đăng nhập Google không hợp lệ hoặc đã hết hạn. Vui lòng thử lại.");
    }
    Object firebaseClaim = token.getClaims().get("firebase");
    if (!(firebaseClaim instanceof Map<?, ?> claims)
        || !"google.com".equals(claims.get("sign_in_provider"))
        || !token.isEmailVerified()
        || token.getEmail() == null || token.getEmail().isBlank()
        || token.getEmail().length() > 150
        || token.getUid() == null || token.getUid().isBlank()) {
      throw new UnauthorizedException("Cần tài khoản Google có email đã được xác minh.");
    }
    return token;
  }

  private void validateAccount(User user) {
    if (user.getStatus() != AccountStatus.ACTIVE) {
      throw new UnauthorizedException("Tài khoản không ở trạng thái hoạt động.");
    }
    if (user.getRoleId() == null
        || !Set.of("STUDENT", "TEACHER").contains(user.getRoleId().getRoleCode())) {
      throw new UnauthorizedException("Đăng nhập Google chỉ dành cho học viên và giáo viên.");
    }
  }

  private User createStudent(FirebaseToken identity, String email) {
    User user = new User();
    user.setUsername("google_" + UUID.randomUUID().toString().replace("-", ""));
    user.setEmail(email);
    String name = identity.getName();
    user.setFullName(name == null || name.isBlank() ? email : name.substring(0, Math.min(150, name.length())));
    // No known password is assigned. The existing reset-password flow can set one later.
    user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
    user.setStatus(AccountStatus.ACTIVE);
    user.setCreatedAt(new Date());
    user.setUpdatedAt(user.getCreatedAt());
    user.setRoleId(roles.findByRoleCodeIgnoreCase("STUDENT")
        .orElseThrow(() -> new IllegalStateException("Không tìm thấy role STUDENT")));
    User saved = users.saveAndFlush(user);
    Student student = new Student();
    student.setStudentCode("HV" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT));
    student.setUserId(saved);
    students.save(student);
    notifications.createWelcomeNotification(saved);
    events.publishEvent(new AccountCreatedMailEvent(saved.getEmail(), saved.getFullName()));
    return saved;
  }
}
