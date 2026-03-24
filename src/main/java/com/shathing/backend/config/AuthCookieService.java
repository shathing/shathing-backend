package com.shathing.backend.config;

import com.shathing.backend.dto.response.AuthTokenResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AuthCookieService {

    private static final String ACCESS_TOKEN = "accessToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String SESSION_COOKIE = "JSESSIONID";

    @Value("${COOKIE_SECURE:false}")
    private boolean cookieSecure;
    @Value("${COOKIE_SAMESITE:Lax}")
    private String cookieSameSite;
    @Value("${COOKIE_DOMAIN:}")
    private String cookieDomain;
    @Value("${JWT_ACCESS_TOKEN_EXPIRATION_SECONDS}")
    private long accessTokenExpirationSeconds;
    @Value("${JWT_REFRESH_TOKEN_EXPIRATION_SECONDS}")
    private long refreshTokenExpirationSeconds;

    public void addAuthCookies(HttpServletResponse response, AuthTokenResponse tokenResponse) {
        addCookie(response, ACCESS_TOKEN, tokenResponse.getAccessToken(), accessTokenExpirationSeconds);
        addCookie(response, REFRESH_TOKEN, tokenResponse.getRefreshToken(), refreshTokenExpirationSeconds);
    }

    public void addAccessTokenCookie(HttpServletResponse response, String accessToken) {
        addCookie(response, ACCESS_TOKEN, accessToken, accessTokenExpirationSeconds);
    }

    public void clearAuthCookies(HttpServletResponse response) {
        deleteCookie(response, ACCESS_TOKEN);
        deleteCookie(response, REFRESH_TOKEN);
    }

    public void clearSessionCookie(HttpServletResponse response) {
        deleteCookie(response, SESSION_COOKIE);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAgeSeconds) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(name, value, Duration.ofSeconds(maxAgeSeconds)).toString());
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(name, "", Duration.ZERO).toString());
    }

    private ResponseCookie buildCookie(String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge);

        if (!cookieDomain.isBlank()) {
            cookieBuilder.domain(cookieDomain);
        }

        return cookieBuilder.build();
    }
}
