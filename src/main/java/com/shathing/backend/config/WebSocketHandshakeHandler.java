package com.shathing.backend.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;

@Component
public class WebSocketHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        Object memberId = attributes.get(WebSocketAuthHandshakeInterceptor.MEMBER_ID_ATTRIBUTE);
        if (memberId == null) {
            return null;
        }
        return new UsernamePasswordAuthenticationToken(memberId, null, Collections.emptyList());
    }
}
