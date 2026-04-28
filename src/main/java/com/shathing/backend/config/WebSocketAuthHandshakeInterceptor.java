package com.shathing.backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.shathing.backend.common.JwtProvider;
import jakarta.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
    private static final String ACCESS_TOKEN_QUERY_PARAM = "access_token";
    private static final String LEGACY_ACCESS_TOKEN_QUERY_PARAM = "accessToken";
    private static final String BEARER_PREFIX = "Bearer ";
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

        ResolvedAccessToken resolvedAccessToken = resolveAccessToken(servletServerHttpRequest);
        if (resolvedAccessToken.token() == null || resolvedAccessToken.token().isBlank()) {
            return false;
        }

        try {
            DecodedJWT decodedJWT = jwtProvider.parseAccessToken(resolvedAccessToken.token());
            Long memberId = Long.parseLong(decodedJWT.getSubject());
            attributes.put(MEMBER_ID_ATTRIBUTE, memberId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private ResolvedAccessToken resolveAccessToken(ServletServerHttpRequest request) {
        String authorizationHeader = request.getServletRequest().getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            String bearerToken = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
            if (!bearerToken.isBlank()) {
                return new ResolvedAccessToken(bearerToken, "authorization-header");
            }
        }

        String queryToken = request.getServletRequest().getParameter(ACCESS_TOKEN_QUERY_PARAM);
        if (queryToken != null && !queryToken.isBlank()) {
            return new ResolvedAccessToken(queryToken, "query-access_token");
        }

        String legacyQueryToken = request.getServletRequest().getParameter(LEGACY_ACCESS_TOKEN_QUERY_PARAM);
        if (legacyQueryToken != null && !legacyQueryToken.isBlank()) {
            return new ResolvedAccessToken(legacyQueryToken, "query-accessToken");
        }

        Cookie[] cookies = request.getServletRequest().getCookies();
        if (cookies == null) {
            return new ResolvedAccessToken(null, "none");
        }

        for (Cookie cookie : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                return new ResolvedAccessToken(cookie.getValue(), "cookie-accessToken");
            }
        }

        return new ResolvedAccessToken(null, "none");
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
    }

    private record ResolvedAccessToken(String token, String source) {
    }
}
