package com.ntt.language_center_management.unit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ntt.language_center_management.entity.Role;
import com.ntt.language_center_management.entity.User;
import com.ntt.language_center_management.enums.AccountStatus;
import com.ntt.language_center_management.filters.JwtFilter;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class JwtFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueFilterChainWhenAuthorizationHeaderIsMissing() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new JwtFilter(mock(JwtUtils.class), mock(UserService.class), mock(ObjectMapper.class))
                .doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void shouldReturnUnauthorizedWhenAuthorizationHeaderIsMalformed() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Basic credentials");

        new JwtFilter(mock(JwtUtils.class), mock(UserService.class), mock(ObjectMapper.class))
                .doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateActiveUserWithNormalizedRoleWhenBearerTokenIsValid() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserService userService = mock(UserService.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer valid-token");
        User user = activeUser(" teacher ");
        when(jwtUtils.validateTokenAndGetUsername("valid-token")).thenReturn("teacher@example.com");
        when(userService.getUserEntityByEmail("teacher@example.com")).thenReturn(user);

        new JwtFilter(jwtUtils, userService, new ObjectMapper()).doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("teacher@example.com");
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_TEACHER");
        assertThat(request.getAttribute("username")).isEqualTo("teacher@example.com");
        verify(chain).doFilter(request, response);
    }

    @Test
    void shouldReturnJsonUnauthorizedWhenTokenValidationFails() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserService userService = mock(UserService.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer expired-token");
        when(jwtUtils.validateTokenAndGetUsername("expired-token"))
            .thenThrow(new IllegalArgumentException("expired"));

        new JwtFilter(jwtUtils, userService, new ObjectMapper()).doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).isEqualTo("application/json;charset=UTF-8");
        assertThat(response.getContentAsString()).contains("\"status\":401").doesNotContain("expired-token");
        verify(chain, never()).doFilter(request, response);
        verify(userService, never()).getUserEntityByEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void shouldRejectInactiveUserAndUserWithoutRole() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserService userService = mock(UserService.class);
        when(jwtUtils.validateTokenAndGetUsername("token")).thenReturn("user@example.com");

        User inactive = activeUser("STUDENT");
        inactive.setStatus(AccountStatus.INACTIVE);
        when(userService.getUserEntityByEmail("user@example.com")).thenReturn(inactive);
        assertUnauthorized(jwtUtils, userService);

        User noRole = activeUser("   ");
        when(userService.getUserEntityByEmail("user@example.com")).thenReturn(noRole);
        assertUnauthorized(jwtUtils, userService);
    }

    @Test
    void shouldNotOverwriteExistingAuthentication() throws Exception {
        JwtUtils jwtUtils = mock(JwtUtils.class);
        UserService userService = mock(UserService.class);
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token");
        when(jwtUtils.validateTokenAndGetUsername("token")).thenReturn("new@example.com");
        when(userService.getUserEntityByEmail("new@example.com")).thenReturn(activeUser("STUDENT"));
        var existing = new UsernamePasswordAuthenticationToken("existing@example.com", null);
        SecurityContextHolder.getContext().setAuthentication(existing);

        new JwtFilter(jwtUtils, userService, new ObjectMapper()).doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        assertThat(request.getAttribute("username")).isNull();
        verify(chain).doFilter(request, response);
    }

    private void assertUnauthorized(JwtUtils jwtUtils, UserService userService) throws Exception {
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("Authorization", "Bearer token");
        new JwtFilter(jwtUtils, userService, new ObjectMapper()).doFilter(request, response, chain);
        assertThat(response.getStatus()).isEqualTo(401);
        verify(chain, never()).doFilter(request, response);
    }

    private User activeUser(String roleCode) {
        User user = new User(1);
        user.setStatus(AccountStatus.ACTIVE);
        user.setRoleId(new Role(2, roleCode, roleCode));
        return user;
    }
}
