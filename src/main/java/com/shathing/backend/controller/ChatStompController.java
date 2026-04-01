package com.shathing.backend.controller;

import com.shathing.backend.dto.request.SendChatMessageRequest;
import com.shathing.backend.dto.response.ChatMessageResponse;
import com.shathing.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatStompController {

    private final ChatService chatService;
    private final SimpMessagingTemplate simpMessagingTemplate;

    @MessageMapping("/chat/rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId,
            @Valid SendChatMessageRequest request,
            Principal principal
    ) {
        Long memberId = Long.parseLong(principal.getName());
        ChatMessageResponse response = chatService.sendMessage(memberId, roomId, request);
        simpMessagingTemplate.convertAndSend("/topic/chat/rooms/" + roomId, response);
    }
}
