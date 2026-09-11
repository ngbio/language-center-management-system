package com.ntt.language_center_management.service;

import com.ntt.language_center_management.dto.response.FirebaseChatTokenResponse;
import java.security.Principal;

public interface FirebaseChatService {
  FirebaseChatTokenResponse createToken(Principal principal);
}
