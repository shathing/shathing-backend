package com.shathing.backend.common;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.shathing.backend.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final Algorithm algorithm;
    @Value("${APP_NAME}")
    private String appName;
    @Value("${JWT_ACCESS_TOKEN_EXPIRATION_SECONDS}")
    private long accessTokenExpirationSeconds;
    @Value("${JWT_REFRESH_TOKEN_EXPIRATION_SECONDS}")
    private long refreshTokenExpirationSeconds;

    public String createAccessToken(Member member) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(appName)
                .withSubject(String.valueOf(member.getId()))
                .withClaim("email", member.getEmail())
                .withClaim(TOKEN_TYPE_CLAIM, ACCESS_TOKEN_TYPE)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(accessTokenExpirationSeconds))
                .sign(algorithm);
    }

    public String createRefreshToken(Member member) {
        Instant now = Instant.now();
        return JWT.create()
                .withIssuer(appName)
                .withSubject(String.valueOf(member.getId()))
                .withClaim(TOKEN_TYPE_CLAIM, REFRESH_TOKEN_TYPE)
                .withIssuedAt(now)
                .withExpiresAt(now.plusSeconds(refreshTokenExpirationSeconds))
                .sign(algorithm);
    }

    public DecodedJWT parseAccessToken(String token) {
        return parseByType(token, ACCESS_TOKEN_TYPE);
    }

    public DecodedJWT parseRefreshToken(String token) {
        return parseByType(token, REFRESH_TOKEN_TYPE);
    }

    public boolean isValidAccessToken(String token) {
        return isValid(token, ACCESS_TOKEN_TYPE);
    }

    public boolean isValidRefreshToken(String token) {
        return isValid(token, REFRESH_TOKEN_TYPE);
    }

    private DecodedJWT parseByType(String token, String tokenType) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(appName)
                    .withClaim(TOKEN_TYPE_CLAIM, tokenType)
                    .build();
            return verifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.", e);
        }
    }

    private boolean isValid(String token, String tokenType) {
        try {
            parseByType(token, tokenType);
            return true;
        } catch (TokenExpiredException e) {
            return false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
