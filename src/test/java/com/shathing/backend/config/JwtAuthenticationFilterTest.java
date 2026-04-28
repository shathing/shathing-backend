package com.shathing.backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.shathing.backend.common.JwtProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternalAuthenticatesWithBearerToken() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer header-token");

        when(jwtProvider.parseAccessToken("header-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("1");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(1L);
    }

    @Test
    void doFilterInternalFallsBackToCookieWhenBearerTokenMissing() throws Exception {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtProvider);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "cookie-token"));

        when(jwtProvider.parseAccessToken("cookie-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("2");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(2L);
    }
}
