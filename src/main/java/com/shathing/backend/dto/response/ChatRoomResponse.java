package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ChatRoomResponse {

    private Long id;
    private OtherMemberInfo otherMember;
    private String lastMessage;
    private Instant lastMessageAt;

    @Getter
    @AllArgsConstructor
    public static class OtherMemberInfo {
        private Long id;
        private String username;
    }
}
