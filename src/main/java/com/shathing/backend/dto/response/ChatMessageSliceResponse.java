package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatMessageSliceResponse {

    private List<ChatMessageResponse> items;
    private Long nextCursorId;
    private boolean hasNext;
}
