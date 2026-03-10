package com.shathing.backend.config;

import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class JwtConfig {

    @Bean
    public Algorithm jwtAlgorithm(@Value("${JWT_SECRET}") String jwtSecret) {
        return Algorithm.HMAC256(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
