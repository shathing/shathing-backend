package com.shathing.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleIdTokenVerifierTest {

    @Test
    void verifyReturnsClaimsWhenTokenIsValid() {
        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier(jwtDecoder, "google-client-id", "");
        Jwt jwt = new Jwt(
                "id-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", "https://accounts.google.com",
                        "aud", List.of("google-client-id"),
                        "email", "user@example.com",
                        "email_verified", true
                )
        );

        when(jwtDecoder.decode("id-token")).thenReturn(jwt);

        GoogleIdTokenClaims claims = verifier.verify("id-token");

        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.emailVerified()).isTrue();
    }

    @Test
    void verifyRejectsUnexpectedAudience() {
        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier(jwtDecoder, "google-client-id", "");
        Jwt jwt = new Jwt(
                "id-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", "https://accounts.google.com",
                        "aud", List.of("another-client-id"),
                        "email", "user@example.com",
                        "email_verified", true
                )
        );

        when(jwtDecoder.decode("id-token")).thenReturn(jwt);

        assertThatThrownBy(() -> verifier.verify("id-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Google ID 토큰 audience가 올바르지 않습니다.");
    }

    @Test
    void verifyRejectsUndecodableToken() {
        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier(jwtDecoder, "google-client-id", "");

        when(jwtDecoder.decode("id-token")).thenThrow(new JwtException("bad token"));

        assertThatThrownBy(() -> verifier.verify("id-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("유효하지 않은 Google ID 토큰입니다.");
    }

    @Test
    void verifyAcceptsConfiguredAdditionalAudience() {
        JwtDecoder jwtDecoder = mock(JwtDecoder.class);
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier(
                jwtDecoder,
                "web-client-id",
                "expo-web-client-id,ios-client-id,android-client-id"
        );
        Jwt jwt = new Jwt(
                "id-token",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of(
                        "iss", "https://accounts.google.com",
                        "aud", List.of("android-client-id"),
                        "email", "user@example.com",
                        "email_verified", true
                )
        );

        when(jwtDecoder.decode("id-token")).thenReturn(jwt);

        GoogleIdTokenClaims claims = verifier.verify("id-token");

        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.emailVerified()).isTrue();
    }
}
