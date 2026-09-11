package com.ntt.language_center_management.controller.chat;

import com.ntt.language_center_management.dto.response.ApiResponse;
import com.ntt.language_center_management.dto.response.FirebaseChatTokenResponse;
import com.ntt.language_center_management.service.FirebaseChatService;
import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class FirebaseChatApiController {
  private final FirebaseChatService firebaseChatService;

  public FirebaseChatApiController(FirebaseChatService firebaseChatService) {
    this.firebaseChatService = firebaseChatService;
  }

  @PostMapping("/firebase-token")
  public ApiResponse<FirebaseChatTokenResponse> createFirebaseToken(Principal principal) {
    return new ApiResponse<>(
        200, "Khởi tạo phiên chat thành công", firebaseChatService.createToken(principal));
  }
}
