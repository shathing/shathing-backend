package com.shathing.backend.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.shathing.backend.common.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Cookie accessTokenCookie = WebUtils.getCookie(request, "accessToken");
        if (accessTokenCookie != null) {
            String token = accessTokenCookie.getValue();
            try {
                DecodedJWT decodedJWT = jwtProvider.parseAccessToken(token);
                Long memberId = Long.parseLong(decodedJWT.getSubject());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (IllegalArgumentException ignored) {
                // Leave SecurityContext empty so Spring Security returns 401 on protected endpoints.
            }
        }

        filterChain.doFilter(request, response);
    }
}
