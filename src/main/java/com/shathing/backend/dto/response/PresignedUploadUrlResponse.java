package com.shathing.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PresignedUploadUrlResponse {

    private String key;
    private String uploadUrl;
    private String publicUrl;
}
