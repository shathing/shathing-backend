package com.shathing.backend.service;

import com.shathing.backend.common.JwtProvider;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.shathing.backend.dto.request.SendAuthEmailRequest;
import com.shathing.backend.dto.response.AuthTokenResponse;
import com.shathing.backend.dto.response.MemberResponse;
import com.shathing.backend.entity.EmailAuthToken;
import com.shathing.backend.entity.Member;
import com.shathing.backend.entity.MemberStatus;
import com.shathing.backend.repository.EmailAuthTokenRepository;
import com.shathing.backend.repository.MemberRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class MemberService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JavaMailSender javaMailSender;
    private final JwtProvider jwtProvider;
    private final MemberRepository memberRepository;
    private final EmailAuthTokenRepository emailAuthTokenRepository;
    private final GoogleIdTokenVerifier googleIdTokenVerifier;
    @Value("${app.name}")
    private String appName;
    @Value("${app.frontend-url}")
    private String appFrontendUrl;
    @Value("${auth.token-expiration-seconds}")
    private long authTokenExpirationSeconds;

    @Transactional
    public void sendAuthEmail(SendAuthEmailRequest request) {
        String email = request.getEmail();
        String title = appName + " 회원가입";

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .username(extractUsername(email))
                                .status(MemberStatus.PENDING)
                                .build()
                ));

        if (member.getStatus() == MemberStatus.ACTIVE) {
            title = appName + " 로그인";
        }
        if (member.getStatus() == MemberStatus.SUSPENDED || member.getStatus() == MemberStatus.DELETED) {
            throw new IllegalStateException("로그인할 수 없는 계정 상태입니다.");
        }

        Instant now = Instant.now();
        String rawToken = generateRawToken();
        String tokenHash = sha256(rawToken);
        Instant expiresAt = now.plusSeconds(authTokenExpirationSeconds);

        emailAuthTokenRepository.findByMember_Id(member.getId())
                .ifPresentOrElse(
                        existingToken -> existingToken.refreshToken(tokenHash, expiresAt),
                        () -> emailAuthTokenRepository.save(
                                EmailAuthToken.builder()
                                        .member(member)
                                        .tokenHash(tokenHash)
                                        .expiresAt(expiresAt)
                                        .build()
                        )
                );

        String encodedToken = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String verifyLink = appFrontendUrl + "/auth/email?token=" + encodedToken;

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(message, true);
            String htmlContent = buildAuthEmailHtml(title, verifyLink, rawToken, request.isFromApp());

            messageHelper.setTo(email);
            messageHelper.setSubject(title);
            messageHelper.setText(htmlContent, true);

            javaMailSender.send(message);
        } catch (MessagingException e) {
            System.out.println("e = " + e);
            throw new RuntimeException(e);
        }
    }

    @Transactional
    public AuthTokenResponse verifyAuthEmail(String token) {
        Instant now = Instant.now();
        String tokenHash = sha256(token);

        EmailAuthToken emailAuthToken = emailAuthTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 링크입니다."));

        if (emailAuthToken.isExpired(now)) {
            throw new IllegalArgumentException("만료되었거나 이미 사용된 링크입니다.");
        }

        Member member = emailAuthToken.getMember();
        if (member.getStatus() == MemberStatus.PENDING) {
            member.activate();
        }

        emailAuthTokenRepository.delete(emailAuthToken);
        return issueAuthTokens(member);
    }

    @Transactional
    public AuthTokenResponse loginWithGoogleOAuth(String email, boolean emailVerified) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google 계정 이메일이 없습니다.");
        }
        if (!emailVerified) {
            throw new IllegalArgumentException("Google 이메일 인증이 완료되지 않았습니다.");
        }

        Member member = memberRepository.findByEmail(email)
                .orElseGet(() -> memberRepository.save(
                        Member.builder()
                                .email(email)
                                .username(extractUsername(email))
                                .status(MemberStatus.ACTIVE)
                                .build()
                ));

        if (member.getStatus() == MemberStatus.PENDING) {
            member.activate();
        }
        if (member.getStatus() == MemberStatus.SUSPENDED || member.getStatus() == MemberStatus.DELETED) {
            throw new IllegalStateException("로그인할 수 없는 계정 상태입니다.");
        }

        return issueAuthTokens(member);
    }

    @Transactional
    public AuthTokenResponse loginWithGoogleIdToken(String idToken) {
        GoogleIdTokenClaims claims = googleIdTokenVerifier.verify(idToken);
        return loginWithGoogleOAuth(claims.email(), claims.emailVerified());
    }

    @Transactional(readOnly = true)
    public MemberResponse getMember(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return new MemberResponse(member.getId(),member.getEmail(), member.getUsername());
    }

    @Transactional(readOnly = true)
    public String reissueAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰이 없습니다.");
        }

        DecodedJWT decodedRefreshToken = jwtProvider.parseRefreshToken(refreshToken);
        Long memberId = Long.parseLong(decodedRefreshToken.getSubject());
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (member.getStatus() == MemberStatus.SUSPENDED || member.getStatus() == MemberStatus.DELETED) {
            throw new IllegalStateException("로그인할 수 없는 계정 상태입니다.");
        }

        return jwtProvider.createAccessToken(member);
    }

    private String extractUsername(String email) {
        String[] split = email.split("@");
        return split[0];
    }

    private String buildAuthEmailHtml(String title, String verifyLink, String rawToken, boolean fromApp) {
        if (fromApp) {
            return "<p>" + title + "을(를) 완료하려면 아래 토큰을 복사해 앱에 입력하세요.</p>" +
                    "<p><code>" + rawToken + "</code></p>";
        }
        return "<a href='" + verifyLink + "'>" + title + "</a>";
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    private String sha256(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    @Transactional
    public int cleanupOldEmailAuthTokens() {
        Instant now = Instant.now();
        return emailAuthTokenRepository.deleteByExpiresAtBefore(now);
    }

    private AuthTokenResponse issueAuthTokens(Member member) {
        String accessToken = jwtProvider.createAccessToken(member);
        String refreshToken = jwtProvider.createRefreshToken(member);
        return new AuthTokenResponse(accessToken, refreshToken);
    }
}
