package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class ChatMessageResponse {

    private Long id;
    private Long roomId;
    private SenderInfo sender;
    private String content;
    private Instant createdDate;

    @Getter
    @AllArgsConstructor
    public static class SenderInfo {
        private Long id;
        private String username;
    }
}
