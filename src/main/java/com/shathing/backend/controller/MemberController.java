package com.shathing.backend.controller;

import com.shathing.backend.config.AuthCookieService;
import com.shathing.backend.dto.request.GoogleLoginRequest;
import com.shathing.backend.dto.request.SendAuthEmailRequest;
import com.shathing.backend.dto.request.VerifyAuthEmailRequest;
import com.shathing.backend.dto.response.AuthTokenResponse;
import com.shathing.backend.dto.response.MemberResponse;
import com.shathing.backend.dto.response.VerifyAuthTokenResponse;
import com.shathing.backend.service.MemberService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final static String REFRESH_TOKEN = "refreshToken";
    private final AuthCookieService authCookieService;

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

        authCookieService.addAuthCookies(servletResponse, tokenResponse);
        return ResponseEntity.ok(new VerifyAuthTokenResponse(tokenResponse.getAccessToken()));
    }

    @PostMapping("/auth/google")
    public ResponseEntity<AuthTokenResponse> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse servletResponse
    ) {
        AuthTokenResponse tokenResponse = memberService.loginWithGoogleIdToken(request.getIdToken());
        authCookieService.addAuthCookies(servletResponse, tokenResponse);
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<Void> refreshAccessToken(
            @CookieValue(value = REFRESH_TOKEN, required = false) String refreshToken,
            HttpServletResponse servletResponse
    ) {
        String accessToken = memberService.reissueAccessToken(refreshToken);
        authCookieService.addAccessTokenCookie(servletResponse, accessToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletResponse servletResponse) {
        authCookieService.clearAuthCookies(servletResponse);
        authCookieService.clearSessionCookie(servletResponse);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<MemberResponse> getMember(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(memberService.getMember(memberId));
    }
}
