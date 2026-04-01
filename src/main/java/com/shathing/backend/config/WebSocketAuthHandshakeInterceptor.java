package com.shathing.backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.shathing.backend.common.JwtProvider;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthHandshakeInterceptor implements HandshakeInterceptor {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    static final String MEMBER_ID_ATTRIBUTE = "memberId";

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        if (!(request instanceof ServletServerHttpRequest servletServerHttpRequest)) {
            return false;
        }

        Cookie[] cookies = servletServerHttpRequest.getServletRequest().getCookies();
        if (cookies == null) {
            return false;
        }

        for (Cookie cookie : cookies) {
            if (!ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                continue;
            }
            try {
                DecodedJWT decodedJWT = jwtProvider.parseAccessToken(cookie.getValue());
                attributes.put(MEMBER_ID_ATTRIBUTE, Long.parseLong(decodedJWT.getSubject()));
                return true;
            } catch (IllegalArgumentException ignored) {
                return false;
            }
        }

        return false;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }
}
