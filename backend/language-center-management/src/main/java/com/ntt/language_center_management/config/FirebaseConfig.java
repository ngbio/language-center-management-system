package com.ntt.language_center_management.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FirebaseConfig {
  @Bean
  @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
  FirebaseApp firebaseApp(
      @Value("${firebase.service-account-base64:}") String encodedCredentials,
      @Value("${firebase.database-url:}") String databaseUrl)
      throws IOException {
    if (encodedCredentials == null || encodedCredentials.isBlank()) {
      throw new IllegalStateException("Thiếu FIREBASE_SERVICE_ACCOUNT_JSON_BASE64");
    }
    if (databaseUrl == null || databaseUrl.isBlank()) {
      throw new IllegalStateException("Thiếu FIREBASE_DATABASE_URL");
    }
    byte[] credentials;
    try {
      credentials = Base64.getDecoder().decode(encodedCredentials.replaceAll("\\s", ""));
    } catch (IllegalArgumentException exception) {
      throw new IllegalStateException("FIREBASE_SERVICE_ACCOUNT_JSON_BASE64 không hợp lệ", exception);
    }
    FirebaseOptions options =
        FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(new ByteArrayInputStream(credentials)))
            .setDatabaseUrl(databaseUrl)
            .build();
    return FirebaseApp.initializeApp(options, "language-center-chat");
  }

  @Bean
  @ConditionalOnProperty(name = "firebase.enabled", havingValue = "true")
  FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
    return FirebaseAuth.getInstance(firebaseApp);
  }
}
