package com.ntt.language_center_management.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.ntt.language_center_management.filters.JwtFilter;
import com.ntt.language_center_management.service.UserService;
import com.ntt.language_center_management.util.JwtUtils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

class JwtFilterTest {

    @Test
    void requestWithoutAuthorizationHeaderContinuesFilterChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        new JwtFilter(mock(JwtUtils.class), mock(UserService.class), mock(ObjectMapper.class))
                .doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void malformedAuthorizationHeaderReturnsUnauthorized() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Basic credentials");

        new JwtFilter(mock(JwtUtils.class), mock(UserService.class), mock(ObjectMapper.class))
                .doFilter(request, response, chain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(chain, never()).doFilter(request, response);
    }
}
