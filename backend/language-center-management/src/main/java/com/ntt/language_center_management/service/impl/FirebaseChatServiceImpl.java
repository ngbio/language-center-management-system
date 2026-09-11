package com.ntt.language_center_management.service.impl;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.ntt.language_center_management.dto.response.FirebaseChatTokenResponse;
import com.ntt.language_center_management.dto.response.UserResponse;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.exception.ChatUnavailableException;
import com.ntt.language_center_management.exception.ForbiddenException;
import com.ntt.language_center_management.exception.UnauthorizedException;
import com.ntt.language_center_management.repository.UserRepository;
import com.ntt.language_center_management.service.FirebaseChatService;
import com.ntt.language_center_management.service.UserService;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class FirebaseChatServiceImpl implements FirebaseChatService {
  private static final Set<String> CHAT_ROLES = Set.of("STUDENT", "CONSULTANT");
  private final UserService userService;
  private final UserRepository userRepository;
  private final Optional<FirebaseAuth> firebaseAuth;

  public FirebaseChatServiceImpl(
      UserService userService,
      UserRepository userRepository,
      Optional<FirebaseAuth> firebaseAuth) {
    this.userService = userService;
    this.userRepository = userRepository;
    this.firebaseAuth = firebaseAuth;
  }

  @Override
  public FirebaseChatTokenResponse createToken(Principal principal) {
    if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
      throw new UnauthorizedException("Chưa xác thực người dùng");
    }
    UserResponse user = userService.getCurrentUserProfile(principal);
    String role = user.roleCode() == null ? "" : user.roleCode().toUpperCase();
    if (!CHAT_ROLES.contains(role)) {
      throw new ForbiddenException("Chỉ học viên và nhân viên tư vấn được sử dụng chat");
    }

    FirebaseAuth auth =
        firebaseAuth.orElseThrow(
            () -> new ChatUnavailableException("Chat chưa được cấu hình trên máy chủ"));
    String uid = firebaseUid(user.id());
    User consultant = "STUDENT".equals(role) ? assignConsultant(user.id()) : null;
    String consultantUid = consultant == null ? uid : firebaseUid(consultant.getId());
    String consultantName = consultant == null ? user.fullName() : consultant.getFullName();

    Map<String, Object> claims = new HashMap<>();
    claims.put("role", role);
    claims.put("userId", user.id());
    claims.put("fullName", user.fullName());
    claims.put("consultantUid", consultantUid);
    try {
      String token = auth.createCustomToken(uid, claims);
      return new FirebaseChatTokenResponse(
          token, uid, user.id(), user.fullName(), role, consultantUid, consultantName);
    } catch (FirebaseAuthException exception) {
      throw new ChatUnavailableException("Không thể kết nối dịch vụ chat lúc này", exception);
    }
  }

  private User assignConsultant(Integer studentId) {
    List<User> consultants =
        userRepository.findAllByRoleId_RoleCodeIgnoreCaseAndStatusOrderByIdAsc(
            "CONSULTANT", AccountStatus.ACTIVE);
    if (consultants.isEmpty()) {
      throw new ChatUnavailableException("Hiện chưa có nhân viên tư vấn trực tuyến");
    }
    return consultants.get(Math.floorMod(studentId, consultants.size()));
  }

  private String firebaseUid(Integer userId) {
    return "lcm-user-" + userId;
  }
}
