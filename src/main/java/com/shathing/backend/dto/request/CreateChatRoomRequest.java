package com.shathing.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateChatRoomRequest {

    @NotNull
    private Long otherMemberId;
}
