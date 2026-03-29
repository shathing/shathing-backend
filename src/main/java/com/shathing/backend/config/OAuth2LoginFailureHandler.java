package com.shathing.backend.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final AuthCookieService authCookieService;
    @Value("${app.frontend-url}")
    private String appFrontendUrl;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        invalidateSession(request);
        authCookieService.clearAuthCookies(response);
        authCookieService.clearSessionCookie(response);
        response.sendRedirect(buildFailureRedirectUrl());
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    private String buildFailureRedirectUrl() {
        return UriComponentsBuilder.fromUriString(appFrontendUrl)
                .queryParam("error", "google_oauth_failed")
                .build()
                .toUriString();
    }
}
