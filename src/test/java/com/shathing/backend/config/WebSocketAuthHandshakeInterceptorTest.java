package com.shathing.backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.shathing.backend.common.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WebSocketAuthHandshakeInterceptorTest {

    @Test
    void beforeHandshakeAuthenticatesWithAccessTokenQuery() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        WebSocketAuthHandshakeInterceptor interceptor = new WebSocketAuthHandshakeInterceptor(jwtProvider);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("access_token", "query-token");
        Map<String, Object> attributes = new HashMap<>();

        when(jwtProvider.parseAccessToken("query-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("1");

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes.get(WebSocketAuthHandshakeInterceptor.MEMBER_ID_ATTRIBUTE)).isEqualTo(1L);
    }

    @Test
    void beforeHandshakeAuthenticatesWithBearerHeader() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        WebSocketAuthHandshakeInterceptor interceptor = new WebSocketAuthHandshakeInterceptor(jwtProvider);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.addHeader("Authorization", "Bearer header-token");
        Map<String, Object> attributes = new HashMap<>();

        when(jwtProvider.parseAccessToken("header-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("2");

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes.get(WebSocketAuthHandshakeInterceptor.MEMBER_ID_ATTRIBUTE)).isEqualTo(2L);
    }

    @Test
    void beforeHandshakeRejectsMissingToken() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        WebSocketAuthHandshakeInterceptor interceptor = new WebSocketAuthHandshakeInterceptor(jwtProvider);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(result).isFalse();
        assertThat(attributes).isEmpty();
    }

    @Test
    void beforeHandshakeAuthenticatesWithLegacyAccessTokenQuery() {
        JwtProvider jwtProvider = mock(JwtProvider.class);
        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        WebSocketAuthHandshakeInterceptor interceptor = new WebSocketAuthHandshakeInterceptor(jwtProvider);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setParameter("accessToken", "legacy-query-token");
        Map<String, Object> attributes = new HashMap<>();

        when(jwtProvider.parseAccessToken("legacy-query-token")).thenReturn(decodedJWT);
        when(decodedJWT.getSubject()).thenReturn("4");

        boolean result = interceptor.beforeHandshake(
                new ServletServerHttpRequest(servletRequest),
                mock(ServerHttpResponse.class),
                mock(WebSocketHandler.class),
                attributes
        );

        assertThat(result).isTrue();
        assertThat(attributes.get(WebSocketAuthHandshakeInterceptor.MEMBER_ID_ATTRIBUTE)).isEqualTo(4L);
    }
}
