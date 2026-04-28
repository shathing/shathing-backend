package com.shathing.backend.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GoogleIdTokenVerifier {

    private static final Set<String> ALLOWED_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final JwtDecoder googleIdTokenDecoder;
    private final Set<String> googleClientIds;

    public GoogleIdTokenVerifier(
            @Qualifier("googleIdTokenDecoder") JwtDecoder googleIdTokenDecoder,
            @Value("${spring.security.oauth2.client.registration.google.client-id}") String googleClientId,
            @Value("${app.google.allowed-client-ids:}") String additionalGoogleClientIds
    ) {
        this.googleIdTokenDecoder = googleIdTokenDecoder;
        this.googleClientIds = Stream.concat(
                        Stream.of(googleClientId),
                        Arrays.stream(additionalGoogleClientIds.split(","))
                )
                .map(String::trim)
                .filter(clientId -> !clientId.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public GoogleIdTokenClaims verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new IllegalArgumentException("Google ID 토큰이 없습니다.");
        }

        Jwt jwt;
        try {
            jwt = googleIdTokenDecoder.decode(idToken);
        } catch (JwtException exception) {
            throw new IllegalArgumentException("유효하지 않은 Google ID 토큰입니다.", exception);
        }

        validateIssuer(jwt.getIssuer());
        validateAudience(jwt.getAudience());

        return new GoogleIdTokenClaims(
                jwt.getClaimAsString("email"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))
        );
    }

    private void validateIssuer(URL issuer) {
        String issuerValue = issuer == null ? null : issuer.toString();
        if (!ALLOWED_ISSUERS.contains(issuerValue)) {
            throw new IllegalArgumentException("Google ID 토큰 issuer가 올바르지 않습니다.");
        }
    }

    private void validateAudience(List<String> audience) {
        if (audience == null || audience.stream().noneMatch(googleClientIds::contains)) {
            throw new IllegalArgumentException("Google ID 토큰 audience가 올바르지 않습니다.");
        }
    }
}
