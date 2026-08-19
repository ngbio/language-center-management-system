package com.ntt.language_center_management.filters;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class JwtFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public JwtFilter(JwtUtils jwtUtils, UserService userService, ObjectMapper objectMapper) {
        this.jwtUtils = jwtUtils;
        this.userService = userService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws IOException, ServletException {
        String header = request.getHeader("Authorization");

        // Endpoint nào bắt buộc đăng nhập sẽ do SecurityConfig quyết định.
        if (!StringUtils.hasText(header)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!header.startsWith(BEARER_PREFIX)) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Authorization header không hợp lệ");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token không được để trống");
            return;
        }

        try {
            String email = jwtUtils.validateTokenAndGetUsername(token);
            User user = userService.getUserEntityByEmail(email);

            if (user.getStatus() != AccountStatus.ACTIVE) {
                throw new IllegalArgumentException("Tài khoản không hoạt động");
            }

            String roleCode = user.getRoleId().getRoleCode();
            if (!StringUtils.hasText(roleCode)) {
                throw new IllegalArgumentException("Tài khoản chưa được cấp quyền");
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                String normalizedRole = roleCode.trim().toUpperCase(Locale.ROOT);
                String authority = normalizedRole.startsWith("ROLE_")
                        ? normalizedRole
                        : "ROLE_" + normalizedRole;

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user,
                                null,
                                List.of(new SimpleGrantedAuthority(authority)));
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                request.setAttribute("username", email);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception exception) {
            SecurityContextHolder.clearContext();
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token không hợp lệ, đã hết hạn hoặc tài khoản không khả dụng");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendJsonError(HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "status", status,
                "message", message));
    }
}
