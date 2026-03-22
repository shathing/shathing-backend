package com.shathing.backend.controller;

import com.shathing.backend.dto.request.SendAuthEmailRequest;
import com.shathing.backend.dto.request.VerifyAuthEmailRequest;
import com.shathing.backend.dto.response.AuthTokenResponse;
import com.shathing.backend.dto.response.MemberResponse;
import com.shathing.backend.dto.response.VerifyAuthTokenResponse;
import com.shathing.backend.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
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


    private final static String ACCESS_TOKEN = "accessToken";
    private final static String REFRESH_TOKEN = "refreshToken";

    @PostMapping("/auth/send-email")
    public ResponseEntity<Void> sendAuthEmail(@Valid @RequestBody SendAuthEmailRequest request) {
        memberService.sendAuthEmail(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/verify-token")
    public ResponseEntity<VerifyAuthTokenResponse> verifyAuthToken(
            @Valid @RequestBody VerifyAuthEmailRequest request,
            HttpServletResponse servletResponse
    ) {
        AuthTokenResponse tokenResponse = memberService.verifyAuthEmail(request.getToken());

        servletResponse.addHeader(HttpHeaders.SET_COOKIE,
                buildTokenCookie(ACCESS_TOKEN, tokenResponse.getAccessToken(), accessTokenExpirationSeconds).toString());
        servletResponse.addHeader(HttpHeaders.SET_COOKIE,
                buildTokenCookie(REFRESH_TOKEN, tokenResponse.getRefreshToken(), refreshTokenExpirationSeconds).toString());
        return ResponseEntity.ok(new VerifyAuthTokenResponse(tokenResponse.getAccessToken()));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<Void> refreshAccessToken(
            @CookieValue(value = REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse servletResponse
    ) {
        String accessToken = memberService.reissueAccessToken(refreshToken);
        servletResponse.addHeader(HttpHeaders.SET_COOKIE,
                buildTokenCookie(ACCESS_TOKEN, accessToken, accessTokenExpirationSeconds).toString());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMember(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getMember(memberId));
    }

    private ResponseCookie buildTokenCookie(String name, String value, long maxAgeSeconds) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds));

        if (!cookieDomain.isBlank()) {
            cookieBuilder.domain(cookieDomain);
        }

        return cookieBuilder.build();
    }
}
