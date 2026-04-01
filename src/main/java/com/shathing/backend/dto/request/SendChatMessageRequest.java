package com.shathing.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendChatMessageRequest {

    @NotBlank
    @Size(max = 2000)
    private String content;
}
