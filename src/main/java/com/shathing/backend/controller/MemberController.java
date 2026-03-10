package com.shathing.backend.controller;

import com.shathing.backend.dto.request.SendAuthEmailRequest;
import com.shathing.backend.dto.request.VerifyAuthEmailRequest;
import com.shathing.backend.dto.response.AuthTokenResponse;
import com.shathing.backend.dto.response.VerifyAuthTokenResponse;
import com.shathing.backend.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    @Value("${JWT_REFRESH_TOKEN_EXPIRATION_SECONDS}")
    private long refreshTokenExpirationSeconds;

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

        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokenResponse.getRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ofSeconds(refreshTokenExpirationSeconds))
                .build();

        servletResponse.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return ResponseEntity.ok(new VerifyAuthTokenResponse(tokenResponse.getAccessToken()));
    }
}
