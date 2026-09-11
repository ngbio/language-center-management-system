package com.ntt.language_center_management.unit.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.controller.chat.FirebaseChatApiController;
import com.ntt.language_center_management.dto.response.FirebaseChatTokenResponse;
import com.ntt.language_center_management.service.FirebaseChatService;
import java.security.Principal;
import org.junit.jupiter.api.Test;

class FirebaseChatApiControllerTest {

  @Test
  void shouldReturnFirebaseIdentityWhenAuthenticatedUserStartsChat() {
    FirebaseChatService service = mock(FirebaseChatService.class);
    FirebaseChatApiController controller = new FirebaseChatApiController(service);
    Principal principal = () -> "student@example.com";
    FirebaseChatTokenResponse token =
        new FirebaseChatTokenResponse(
            "custom-token",
            "lcm-user-5",
            5,
            "Student",
            "STUDENT",
            "lcm-user-2",
            "Consultant");
    when(service.createToken(principal)).thenReturn(token);

    var response = controller.createFirebaseToken(principal);

    assertThat(response.status()).isEqualTo(200);
    assertThat(response.data()).isEqualTo(token);
    verify(service).createToken(principal);
  }
}
