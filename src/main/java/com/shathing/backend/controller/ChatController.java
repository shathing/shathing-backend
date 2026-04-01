package com.shathing.backend.controller;

import com.shathing.backend.dto.request.CreateChatRoomRequest;
import com.shathing.backend.dto.response.ChatMessageSliceResponse;
import com.shathing.backend.dto.response.ChatRoomResponse;
import com.shathing.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/chat/rooms")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody CreateChatRoomRequest request
    ) {
        return ResponseEntity.ok(chatService.createOrGetDirectRoom(memberId, request.getOtherMemberId()));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/chat/rooms")
    public ResponseEntity<List<ChatRoomResponse>> getChatRooms(@AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(chatService.getChatRooms(memberId));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/chat/rooms/{roomId}/messages")
    public ResponseEntity<ChatMessageSliceResponse> getChatMessages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long beforeMessageId,
            @RequestParam(defaultValue = "30") int size
    ) {
        return ResponseEntity.ok(chatService.getChatMessages(memberId, roomId, beforeMessageId, size));
    }
}
