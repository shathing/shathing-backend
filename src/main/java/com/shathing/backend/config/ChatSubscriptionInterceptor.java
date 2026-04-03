package com.shathing.backend.config;

import com.shathing.backend.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ChatSubscriptionInterceptor implements ChannelInterceptor {

    private static final Pattern CHAT_ROOM_TOPIC_PATTERN = Pattern.compile("^/topic/chat/rooms/(\\d+)$");

    private final ChatRoomRepository chatRoomRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination == null) {
            return message;
        }

        Matcher matcher = CHAT_ROOM_TOPIC_PATTERN.matcher(destination);
        if (!matcher.matches()) {
            return message;
        }

        Principal principal = accessor.getUser();
        if (principal == null || principal.getName() == null) {
            throw new AccessDeniedException("Unauthorized");
        }

        Long memberId;
        try {
            memberId = Long.parseLong(principal.getName());
        } catch (NumberFormatException exception) {
            throw new AccessDeniedException("Unauthorized");
        }

        Long roomId = Long.parseLong(matcher.group(1));
        if (chatRoomRepository.countAccessibleRoom(roomId, memberId) == 0) {
            throw new AccessDeniedException("Forbidden");
        }

        return message;
    }
}
