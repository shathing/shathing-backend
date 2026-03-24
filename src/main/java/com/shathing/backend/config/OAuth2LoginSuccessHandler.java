package com.shathing.backend.config;

import com.shathing.backend.dto.response.AuthTokenResponse;
import com.shathing.backend.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final MemberService memberService;
    private final AuthCookieService authCookieService;
    @Value("${APP_FRONTEND_URL}")
    private String appFrontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        try {
            String email = oAuth2User.getAttribute("email");
            boolean emailVerified = resolveBoolean(oAuth2User.getAttribute("email_verified"));
            AuthTokenResponse tokenResponse = memberService.loginWithGoogleOAuth(email, emailVerified);

            authCookieService.addAuthCookies(response, tokenResponse);
            authCookieService.clearSessionCookie(response);
            invalidateSession(request);
            response.sendRedirect(appFrontendUrl);
        } catch (RuntimeException e) {
            log.warn("Google OAuth login post-processing failed", e);
            invalidateSession(request);
            authCookieService.clearAuthCookies(response);
            authCookieService.clearSessionCookie(response);
            response.sendRedirect(appFrontendUrl + "?error=google_oauth_failed");
        }
    }

    private boolean resolveBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(String.valueOf(value));
    }

    private void invalidateSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
