package com.ntt.language_center_management.config;

import com.ntt.language_center_management.filters.JwtFilter;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(allowedOrigins);
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    configuration.setExposedHeaders(List.of("Authorization"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", configuration);
    return source;
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CorsConfigurationSource corsConfigurationSource,
      JwtUtils jwtUtils,
      UserService userService,
      ObjectMapper objectMapper)
      throws Exception {
    JwtFilter jwtFilter = new JwtFilter(jwtUtils, userService, objectMapper);

    http.securityMatcher("/api/**")
        .cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exception ->
                exception
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(401);
                          response.setContentType("application/json;charset=UTF-8");
                          objectMapper.writeValue(
                              response.getWriter(),
                              Map.of(
                                  "status",
                                  401,
                                  "message",
                                  "Chưa xác thực hoặc thiếu Bearer token"));
                        })
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) -> {
                          response.setStatus(403);
                          response.setContentType("application/json;charset=UTF-8");
                          objectMapper.writeValue(
                              response.getWriter(),
                              Map.of("status", 403, "message", "Không có quyền truy cập"));
                        }))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                    .permitAll()
                    .requestMatchers("/", "/error", "/css/**", "/js/**", "/images/**")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/admin/auth/login")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/classes/*/schedules",
                        "/api/classes/*/lessons/generate")
                    .hasAnyRole("ADMIN", "CONSULTANT")
                    .requestMatchers(HttpMethod.PUT, "/api/schedules/**")
                    .hasAnyRole("ADMIN", "CONSULTANT")
                    .requestMatchers(HttpMethod.DELETE, "/api/schedules/**")
                    .hasAnyRole("ADMIN", "CONSULTANT")
                    .requestMatchers(HttpMethod.GET, "/api/classes/*/lessons")
                    .authenticated()
                    .requestMatchers(HttpMethod.PUT, "/api/lessons/*")
                    .hasAnyRole("ADMIN", "CONSULTANT", "TEACHER")
                    .requestMatchers(
                        HttpMethod.PATCH,
                        "/api/lessons/*/reschedule",
                        "/api/lessons/*/cancel")
                    .hasAnyRole("ADMIN", "CONSULTANT")
                    .requestMatchers(HttpMethod.PATCH, "/api/admin/classes/*/status")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/admin/classes/**")
                    .hasAnyRole("ADMIN", "CONSULTANT")
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/auth/me")
                    .authenticated()
                    .requestMatchers("/api/auth/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/classes/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/teachers/me/classes")
                    .hasRole("TEACHER")
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/courses/**",
                        "/api/languages/**",
                        "/api/levels/**",
                        "/api/rooms/**")
                    .permitAll()
                    .requestMatchers(
                        "/api/courses/**", "/api/languages/**", "/api/levels/**", "/api/rooms/**")
                    .hasRole("ADMIN")
                    .requestMatchers(
                        "/api/secure", "/api/secure/**",
                        "/api/users/secure", "/api/users/secure/**")
                    .authenticated()
                    .anyRequest()
                    .authenticated())
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
